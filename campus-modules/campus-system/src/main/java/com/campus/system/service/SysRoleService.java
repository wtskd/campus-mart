package com.campus.system.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.entity.SysRole;

import java.util.List;

/**
 * 角色管理服务。
 *
 * @author campus-mart
 */
public interface SysRoleService {

    PageResult<SysRole> page(PageQuery pageQuery, String roleName, String roleKey, String status);

    SysRole detail(Long roleId);

    void add(SysRole role);

    void update(SysRole role);

    void remove(String roleIds);

    void changeStatus(Long roleId, String status);

    /** 全部启用角色，用户表单下拉用 */
    List<SysRole> optionSelect();

    /** 角色已授权的菜单ID */
    List<Long> menuIds(Long roleId);

    /** 整体重写角色菜单授权 */
    void assignMenus(Long roleId, List<Long> menuIds);
}
