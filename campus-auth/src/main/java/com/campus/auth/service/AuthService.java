package com.campus.auth.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import com.campus.api.dto.LoginInfoDTO;
import com.campus.api.dto.MenuNodeDTO;
import com.campus.api.feign.SystemMenuClient;
import com.campus.api.feign.SystemUserClient;
import com.campus.auth.dto.LoginRequest;
import com.campus.auth.dto.LoginResult;
import com.campus.common.core.domain.Result;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.LoginUser;
import com.campus.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 登录态缓存 key 前缀（网关 AuthGlobalFilter 使用同一前缀） */
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    /** 验证码 key 前缀 */
    public static final String CAPTCHA_KEY = "captcha:";
    /** 登录失败次数 key 前缀 */
    public static final String LOGIN_FAIL_KEY = "login:fail:";

    private static final int MAX_FAIL = 5;
    private static final int FAIL_LOCK_SECONDS = 300;

    private final SystemUserClient userClient;
    private final SystemMenuClient menuClient;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${campus.auth.captcha-enabled:true}")
    private boolean captchaEnabled;

    /** 图形验证码 */
    public Map<String, String> captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 40);
        String key = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(CAPTCHA_KEY + key, captcha.getCode(), 120, TimeUnit.SECONDS);
        Map<String, String> data = new LinkedHashMap<>(2);
        data.put("key", key);
        data.put("image", "data:image/png;base64," + captcha.getImageBase64());
        return data;
    }

    /** 登录（后台管理端，需要图形验证码） */
    public LoginResult login(LoginRequest req, String clientIp) {
        return login(req, clientIp, true);
    }

    /**
     * 登录
     *
     * @param checkCaptcha 是否校验图形验证码（小程序/App 端没有验证码输入框，传 false）
     */
    public LoginResult login(LoginRequest req, String clientIp, boolean checkCaptcha) {
        if (checkCaptcha) {
            verifyCaptcha(req);
        }
        checkLocked(req.getUsername());

        LoginInfoDTO info = getLoginInfo(req.getUsername());
        if (info == null || StrUtil.isBlank(info.getPassword())) {
            throw new ServiceException(400, "用户名或密码错误");
        }
        if ("2".equals(info.getDelFlag())) {
            throw new ServiceException(400, "账号不存在");
        }
        if ("1".equals(info.getStatus())) {
            throw new ServiceException(400, "账号已停用，请联系管理员");
        }
        if (!BCrypt.checkpw(req.getPassword(), info.getPassword())) {
            long fails = increaseFail(req.getUsername());
            throw new ServiceException(400, "用户名或密码错误，还可尝试 " + Math.max(0, MAX_FAIL - fails) + " 次");
        }
        stringRedisTemplate.delete(LOGIN_FAIL_KEY + req.getUsername());

        String token = jwtUtils.createToken(info.getUserId(), info.getUsername());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(info.getUserId());
        loginUser.setUsername(info.getUsername());
        loginUser.setNickname(info.getNickname());
        loginUser.setDeptId(info.getDeptId());
        loginUser.setDeptName(info.getDeptName());
        loginUser.setAvatar(info.getAvatar());
        loginUser.setRoles(info.getRoles());
        loginUser.setPerms(info.getPerms() == null ? new HashSet<>() : new HashSet<>(info.getPerms()));
        loginUser.setToken(token);
        loginUser.setExpireAt(System.currentTimeMillis() + jwtUtils.getExpireSeconds() * 1000);

        // 登录态存 Redis：网关每次请求从这里取权限，做到改权限/退出即时生效
        stringRedisTemplate.opsForValue().set(LOGIN_TOKEN_KEY + info.getUserId(),
                JSONUtil.toJsonStr(loginUser), jwtUtils.getExpireSeconds(), TimeUnit.SECONDS);

        try {
            userClient.recordLogin(info.getUserId(), clientIp);
        } catch (Exception e) {
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
        log.info("用户登录成功: {} ({})", info.getUsername(), clientIp);
        return new LoginResult(token, jwtUtils.getExpireSeconds(), loginUser);
    }

    /** 退出登录 */
    public void logout(Long userId) {
        if (userId != null) {
            stringRedisTemplate.delete(LOGIN_TOKEN_KEY + userId);
        }
    }

    /** 当前登录用户信息 */
    public LoginUser me(Long userId) {
        String json = stringRedisTemplate.opsForValue().get(LOGIN_TOKEN_KEY + userId);
        if (StrUtil.isBlank(json)) {
            throw new ServiceException(401, "登录已过期，请重新登录");
        }
        return JSONUtil.toBean(json, LoginUser.class);
    }

    /** 动态菜单树 */
    public List<MenuNodeDTO> menus(Long userId) {
        Result<List<MenuNodeDTO>> r = menuClient.getUserMenuTree(userId);
        if (r == null || !r.isSuccess()) {
            throw new ServiceException("菜单加载失败");
        }
        return r.getData();
    }

    private LoginInfoDTO getLoginInfo(String username) {
        try {
            Result<LoginInfoDTO> r = userClient.getLoginInfoByUsername(username);
            if (r == null) {
                throw new ServiceException("用户服务无响应");
            }
            if (!r.isSuccess()) {
                throw new ServiceException(r.getCode(), r.getMsg());
            }
            return r.getData();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用用户服务失败", e);
            throw new ServiceException(500, "用户服务暂时不可用，请稍后再试");
        }
    }

    private void verifyCaptcha(LoginRequest req) {
        if (!captchaEnabled) {
            return;
        }
        if (StrUtil.isBlank(req.getCaptchaKey()) || StrUtil.isBlank(req.getCaptchaCode())) {
            throw new ServiceException(400, "请输入验证码");
        }
        String key = CAPTCHA_KEY + req.getCaptchaKey();
        String code = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);
        if (code == null || !code.equalsIgnoreCase(req.getCaptchaCode().trim())) {
            throw new ServiceException(400, "验证码错误或已过期");
        }
    }

    private void checkLocked(String username) {
        String v = stringRedisTemplate.opsForValue().get(LOGIN_FAIL_KEY + username);
        if (v != null && Integer.parseInt(v) >= MAX_FAIL) {
            throw new ServiceException(429, "登录失败次数过多，请 5 分钟后再试");
        }
    }

    private long increaseFail(String username) {
        String key = LOGIN_FAIL_KEY + username;
        Long n = stringRedisTemplate.opsForValue().increment(key);
        if (n != null && n == 1L) {
            stringRedisTemplate.expire(key, FAIL_LOCK_SECONDS, TimeUnit.SECONDS);
        }
        return n == null ? 1 : n;
    }
}
