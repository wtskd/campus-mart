package com.campus.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户上下文拦截器
 *
 * <p>网关完成令牌校验后，把用户身份写入请求头；本拦截器解析请求头还原 {@link LoginUser}，
 * 供业务代码通过 {@link SecurityUtils} 获取当前用户。</p>
 *
 * <p>内部服务直连（无网关头）时上下文为空，需要登录的接口会返回 401。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_NICKNAME = "X-Nickname";
    public static final String HEADER_DEPT_ID = "X-Dept-Id";
    public static final String HEADER_DEPT_NAME = "X-Dept-Name";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_PERMS = "X-Perms";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(HEADER_USER_ID);
        if (StringUtils.hasText(userId)) {
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(userId));
            user.setUsername(decode(request.getHeader(HEADER_USERNAME)));
            user.setNickname(decode(request.getHeader(HEADER_NICKNAME)));
            String deptId = request.getHeader(HEADER_DEPT_ID);
            if (StringUtils.hasText(deptId)) {
                user.setDeptId(Long.valueOf(deptId));
            }
            user.setDeptName(decode(request.getHeader(HEADER_DEPT_NAME)));

            String roles = request.getHeader(HEADER_ROLES);
            if (StringUtils.hasText(roles)) {
                List<String> roleList = Arrays.asList(roles.split(","));
                user.setRoles(roleList);
            }
            String perms = request.getHeader(HEADER_PERMS);
            if (StringUtils.hasText(perms)) {
                Set<String> set = new HashSet<>(Arrays.asList(perms.split(",")));
                user.setPerms(set);
            }
            SecurityUtils.set(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityUtils.clear();
    }

    private String decode(String v) {
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return URLDecoder.decode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return v;
        }
    }
}
