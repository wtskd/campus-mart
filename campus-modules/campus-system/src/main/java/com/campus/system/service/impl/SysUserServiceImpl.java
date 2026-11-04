package com.campus.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.api.dto.LoginInfoDTO;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.common.utils.RedisUtils;
import com.campus.system.domain.dto.SysUserSaveDTO;
import com.campus.system.domain.dto.SysUserVO;
import com.campus.system.domain.dto.UserRoleRef;
import com.campus.system.domain.entity.SysDept;
import com.campus.system.domain.entity.SysLoginLog;
import com.campus.system.domain.entity.SysUser;
import com.campus.system.mapper.SysDeptMapper;
import com.campus.system.mapper.SysLoginLogMapper;
import com.campus.system.mapper.SysMenuCustomMapper;
import com.campus.system.mapper.SysUserCustomMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.mapper.SysUserRoleMapper;
import com.campus.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户管理实现。
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 超级管理员固定 ID：种子数据里 admin 的 user_id */
    private static final long SUPER_ADMIN_ID = 1L;

    /** 与 campus-auth 约定的登录态 key，停用用户时必须删掉，否则旧令牌在过期前依然可用 */
    private static final String LOGIN_TOKEN_PREFIX = "login:token:";

    private static final String STATUS_ENABLED = "0";
    private static final String STATUS_DISABLED = "1";

    private final SysUserMapper userMapper;
    private final SysUserCustomMapper userCustomMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuCustomMapper menuCustomMapper;
    private final SysDeptMapper deptMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<SysUserVO> page(PageQuery pageQuery, String username, String nickname,
                                      String phone, String status, Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                .like(StrUtil.isNotBlank(nickname), SysUser::getNickname, nickname)
                .like(StrUtil.isNotBlank(phone), SysUser::getPhone, phone)
                .eq(StrUtil.isNotBlank(status), SysUser::getStatus, status)
                .eq(deptId != null, SysUser::getDeptId, deptId)
                // 固定按主键升序：列表页需要稳定顺序，且避免前端传入任意列名造成 order by 注入
                .orderByAsc(SysUser::getUserId);
        Page<SysUser> page = userMapper.selectPage(pageQuery.toPage(), wrapper);

        List<SysUser> records = page.getRecords();
        Map<Long, String> deptNames = loadDeptNames(records);
        Map<Long, List<Long>> roleMap = loadRoleIds(records.stream().map(SysUser::getUserId).toList());
        return PageResult.of(page, user -> toVO(user, deptNames, roleMap));
    }

    @Override
    public SysUserVO detail(Long userId) {
        SysUser user = userMapper.selectById(userId);
        ServiceException.throwIf(user == null, "用户不存在");
        Map<Long, String> deptNames = loadDeptNames(Collections.singletonList(user));
        Map<Long, List<Long>> roleMap = loadRoleIds(Collections.singletonList(user.getUserId()));
        return toVO(user, deptNames, roleMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserSaveDTO dto) {
        ServiceException.throwIf(StrUtil.isBlank(dto.getUsername()), "用户名不能为空");
        ServiceException.throwIf(StrUtil.isBlank(dto.getNickname()), "用户昵称不能为空");
        // 连逻辑删除的行一起查：username 唯一索引不区分 del_flag，否则会出现“校验通过但插入冲突”
        ServiceException.throwIf(!userCustomMapper.selectUserIdsByUsername(dto.getUsername()).isEmpty(),
                "登录账号已存在");

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setNickname(dto.getNickname().trim());
        user.setDeptId(dto.getDeptId());
        user.setUserType(StrUtil.blankToDefault(dto.getUserType(), "00"));
        user.setGender(StrUtil.blankToDefault(dto.getGender(), "2"));
        user.setCreditScore(dto.getCreditScore() == null ? 100 : dto.getCreditScore());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStudentNo(dto.getStudentNo());
        user.setRemark(dto.getRemark());
        user.setStatus(StrUtil.blankToDefault(dto.getStatus(), STATUS_ENABLED));
        user.setPassword(BCrypt.hashpw(StrUtil.blankToDefault(dto.getPassword(), "123456")));
        // 逻辑删除字段没有自动填充，插入时必须显式给 '0'，否则新用户会被 @TableLogic 过滤掉
        user.setDelFlag(STATUS_ENABLED);
        userMapper.insert(user);

        saveUserRoles(user.getUserId(), dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserSaveDTO dto) {
        ServiceException.throwIf(dto.getUserId() == null, "用户ID不能为空");
        SysUser exist = userMapper.selectById(dto.getUserId());
        ServiceException.throwIf(exist == null, "用户不存在");

        SysUser user = new SysUser();
        user.setUserId(dto.getUserId());
        // 用户名/密码不在修改接口里更新：用户名有唯一索引且登录态依赖它，密码走重置密码接口
        user.setNickname(dto.getNickname());
        user.setDeptId(dto.getDeptId());
        user.setUserType(dto.getUserType());
        user.setGender(dto.getGender());
        user.setCreditScore(dto.getCreditScore());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setStudentNo(dto.getStudentNo());
        user.setRemark(dto.getRemark());
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(dto.getUserId());
            saveUserRoles(dto.getUserId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String userIds) {
        List<Long> ids = parseIds(userIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的用户");
        ServiceException.throwIf(ids.contains(SUPER_ADMIN_ID), "超级管理员不允许删除");
        Long currentUserId = SecurityUtils.getUserId();
        ServiceException.throwIf(currentUserId != null && ids.contains(currentUserId), "不能删除当前登录用户");

        userMapper.deleteBatchIds(ids);
        // 中间表是物理删除：用户被逻辑删除后关系行必须清理，否则该角色永远无法删除
        userRoleMapper.deleteByUserIds(ids);
    }

    @Override
    public void resetPwd(Long userId, String password) {
        ServiceException.throwIf(userId == null, "用户ID不能为空");
        ServiceException.throwIf(StrUtil.isBlank(password), "新密码不能为空");
        SysUser exist = userMapper.selectById(userId);
        ServiceException.throwIf(exist == null, "用户不存在");

        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(BCrypt.hashpw(password));
        userMapper.updateById(user);
        // 密码已变更，强制让旧登录态失效，要求重新登录
        redisUtils.delete(LOGIN_TOKEN_PREFIX + userId);
    }

    @Override
    public void changeStatus(Long userId, String status) {
        ServiceException.throwIf(userId == null, "用户ID不能为空");
        ServiceException.throwIf(!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status),
                "状态值只能是 0（正常）或 1（停用）");
        SysUser exist = userMapper.selectById(userId);
        ServiceException.throwIf(exist == null, "用户不存在");
        ServiceException.throwIf(SUPER_ADMIN_ID == userId && STATUS_DISABLED.equals(status),
                "超级管理员不允许停用");
        Long currentUserId = SecurityUtils.getUserId();
        ServiceException.throwIf(userId.equals(currentUserId) && STATUS_DISABLED.equals(status),
                "不能停用当前登录用户");

        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        userMapper.updateById(user);

        if (STATUS_DISABLED.equals(status)) {
            // 停用必须同时踢掉登录态：否则用户拿着未过期的 JWT 仍能访问
            redisUtils.delete(LOGIN_TOKEN_PREFIX + userId);
        }
    }

    /* ---------------- 内部接口 ---------------- */

    @Override
    public LoginInfoDTO loginInfoByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return fillLoginInfo(userCustomMapper.selectLoginInfoByUsername(username));
    }

    @Override
    public LoginInfoDTO loginInfoByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return fillLoginInfo(userCustomMapper.selectLoginInfoByUserId(userId));
    }

    @Override
    public List<Long> menuIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        // 超级管理员不受角色菜单限制，直接返回全部菜单，避免漏授权导致后台白屏
        if (isAdminUser(userId)) {
            return menuCustomMapper.selectAllMenuIds();
        }
        return menuCustomMapper.selectMenuIdsByUserId(userId);
    }

    @Override
    public void recordLogin(Long userId, String ip) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            // 用户刚被删除时 auth 仍可能回调，这里直接跳过，不抛异常影响登录流程
            return;
        }
        SysUser update = new SysUser();
        update.setUserId(userId);
        update.setLoginIp(ip);
        update.setLoginDate(LocalDateTime.now());
        userMapper.updateById(update);

        SysLoginLog log = new SysLoginLog();
        log.setUsername(user.getUsername());
        log.setIpaddr(ip);
        log.setLoginLocation(locate(ip));
        log.setBrowser("Unknown");
        log.setOs("Unknown");
        log.setStatus("0");
        log.setMsg("登录成功");
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    /* ---------------- 私有工具 ---------------- */

    private LoginInfoDTO fillLoginInfo(LoginInfoDTO info) {
        if (info == null) {
            return null;
        }
        List<String> roles = userCustomMapper.selectRoleKeysByUserId(info.getUserId());
        info.setRoles(roles);
        if (roles != null && roles.contains("admin")) {
            // 超级管理员用通配符，新增按钮权限不需要再改数据库
            info.setPerms(List.of("*:*:*"));
        } else {
            info.setPerms(menuCustomMapper.selectPermsByUserId(info.getUserId()));
        }
        return info;
    }

    private boolean isAdminUser(Long userId) {
        List<String> roles = userCustomMapper.selectRoleKeysByUserId(userId);
        return roles != null && roles.contains("admin");
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().filter(Objects::nonNull).distinct()
                .forEach(roleId -> userRoleMapper.insertOne(userId, roleId));
    }

    private Map<Long, String> loadDeptNames(List<SysUser> users) {
        List<Long> deptIds = users.stream().map(SysUser::getDeptId)
                .filter(Objects::nonNull).distinct().toList();
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getDeptName, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, List<Long>> loadRoleIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRoleMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleRef::getUserId,
                        Collectors.mapping(UserRoleRef::getRoleId, Collectors.toList())));
    }

    private SysUserVO toVO(SysUser user, Map<Long, String> deptNames, Map<Long, List<Long>> roleMap) {
        SysUserVO vo = new SysUserVO();
        BeanUtil.copyProperties(user, vo, "password");
        vo.setDeptName(deptNames.get(user.getDeptId()));
        vo.setRoleIds(roleMap.getOrDefault(user.getUserId(), new ArrayList<>()));
        return vo;
    }

    private List<Long> parseIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                result.add(Long.valueOf(part.trim()));
            }
        }
        return result;
    }

    /** 粗略的登录地点：内网 IP 直接标注，公网不接第三方 IP 库（实训环境无外网依赖） */
    private String locate(String ip) {
        if (StrUtil.isBlank(ip)) {
            return "未知";
        }
        if (ip.startsWith("127.") || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return "内网IP";
        }
        return "未知";
    }
}
