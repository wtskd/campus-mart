package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.system.domain.entity.SysRole;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysRoleMenuMapper;
import com.campus.system.mapper.SysUserRoleMapper;
import com.campus.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 角色管理实现。
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private static final long SUPER_ADMIN_ROLE_ID = 1L;
    private static final String STATUS_ENABLED = "0";
    private static final String STATUS_DISABLED = "1";

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public PageResult<SysRole> page(PageQuery pageQuery, String roleName, String roleKey, String status) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName)
                .like(StrUtil.isNotBlank(roleKey), SysRole::getRoleKey, roleKey)
                .eq(StrUtil.isNotBlank(status), SysRole::getStatus, status)
                .orderByAsc(SysRole::getRoleSort)
                .orderByAsc(SysRole::getRoleId);
        Page<SysRole> page = roleMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public SysRole detail(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        ServiceException.throwIf(role == null, "角色不存在");
        return role;
    }

    @Override
    public void add(SysRole role) {
        ServiceException.throwIf(StrUtil.isBlank(role.getRoleName()), "角色名称不能为空");
        ServiceException.throwIf(StrUtil.isBlank(role.getRoleKey()), "权限字符不能为空");
        ServiceException.throwIf(existsRoleKey(role.getRoleKey(), null), "权限字符已存在");
        if (role.getRoleSort() == null) {
            role.setRoleSort(0);
        }
        if (StrUtil.isBlank(role.getDataScope())) {
            // 实训项目不做多学院数据隔离，默认全部数据可见
            role.setDataScope("1");
        }
        role.setStatus(StrUtil.blankToDefault(role.getStatus(), STATUS_ENABLED));
        role.setDelFlag(STATUS_ENABLED);
        roleMapper.insert(role);
    }

    @Override
    public void update(SysRole role) {
        ServiceException.throwIf(role.getRoleId() == null, "角色ID不能为空");
        SysRole exist = roleMapper.selectById(role.getRoleId());
        ServiceException.throwIf(exist == null, "角色不存在");
        if (StrUtil.isNotBlank(role.getRoleKey())) {
            ServiceException.throwIf(existsRoleKey(role.getRoleKey(), role.getRoleId()), "权限字符已存在");
        }
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String roleIds) {
        List<Long> ids = parseIds(roleIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的角色");
        ServiceException.throwIf(ids.contains(SUPER_ADMIN_ROLE_ID), "超级管理员角色不允许删除");
        for (Long roleId : ids) {
            // 已分配用户的角色不能删：用户会因此失去全部权限，属于“悬空引用”
            ServiceException.throwIf(userRoleMapper.countByRoleId(roleId) > 0,
                    "角色已分配用户，不能删除");
        }
        roleMapper.deleteBatchIds(ids);
        // 授权关系是物理表，必须一起清理，否则菜单删除校验会误判“已被角色引用”
        ids.forEach(roleMenuMapper::deleteByRoleId);
    }

    @Override
    public void changeStatus(Long roleId, String status) {
        ServiceException.throwIf(roleId == null, "角色ID不能为空");
        ServiceException.throwIf(!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status),
                "状态值只能是 0（正常）或 1（停用）");
        SysRole exist = roleMapper.selectById(roleId);
        ServiceException.throwIf(exist == null, "角色不存在");
        ServiceException.throwIf(SUPER_ADMIN_ROLE_ID == roleId && STATUS_DISABLED.equals(status),
                "超级管理员角色不允许停用");

        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setStatus(status);
        roleMapper.updateById(role);
    }

    @Override
    public List<SysRole> optionSelect() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, STATUS_ENABLED)
                .orderByAsc(SysRole::getRoleSort));
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        ServiceException.throwIf(roleMapper.selectById(roleId) == null, "角色不存在");
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        ServiceException.throwIf(roleId == null, "角色ID不能为空");
        ServiceException.throwIf(roleMapper.selectById(roleId) == null, "角色不存在");

        // 授权是“整体重写”：先删后插，前端每次提交的是全量勾选结果
        roleMenuMapper.deleteByRoleId(roleId);
        List<Long> clean = menuIds == null ? Collections.emptyList()
                : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!clean.isEmpty()) {
            roleMenuMapper.insertBatch(roleId, clean);
        }
    }

    private boolean existsRoleKey(String roleKey, Long excludeRoleId) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeRoleId != null, SysRole::getRoleId, excludeRoleId);
        return roleMapper.selectCount(wrapper) > 0;
    }

    private List<Long> parseIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new java.util.ArrayList<>();
        for (String part : ids.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                result.add(Long.valueOf(part.trim()));
            }
        }
        return result;
    }
}
