package com.campus.system.service;

import com.campus.api.dto.MenuNodeDTO;
import com.campus.system.domain.dto.RoleMenuTreeVO;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysMenu;

import java.util.List;

/**
 * 菜单管理服务：菜单维护 + 动态路由（当前用户菜单树）。
 *
 * @author campus-mart
 */
public interface SysMenuService {

    /** 扁平列表（前端自己建树，必须带 parentId） */
    List<SysMenu> list(String menuName, String status);

    SysMenu detail(Long menuId);

    void add(SysMenu menu);

    void update(SysMenu menu);

    void remove(Long menuId);

    /** 全量菜单下拉树（含按钮，供角色授权勾选） */
    List<TreeSelectVO> treeSelect();

    /** 角色授权页：菜单树 + 已勾选ID */
    RoleMenuTreeVO roleMenuTree(Long roleId);

    /** 当前登录用户的动态路由 */
    List<MenuNodeDTO> routers();

    /** 全量菜单树（前端菜单管理页用） */
    List<MenuNodeDTO> allTree();

    /* ---------------- 内部接口（campus-auth 通过 Feign 调用） ---------------- */

    List<MenuNodeDTO> userMenuTree(Long userId);
}
