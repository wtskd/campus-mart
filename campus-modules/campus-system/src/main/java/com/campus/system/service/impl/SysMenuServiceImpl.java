package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.api.dto.MenuNodeDTO;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.system.domain.dto.RoleMenuTreeVO;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysMenu;
import com.campus.system.mapper.SysMenuCustomMapper;
import com.campus.system.mapper.SysMenuMapper;
import com.campus.system.mapper.SysRoleMenuMapper;
import com.campus.system.mapper.SysUserCustomMapper;
import com.campus.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 菜单管理实现。
 *
 * <p>树结构统一在这里构建：内部接口给 auth 用、/routers 给前端动态路由用、
 * /treeselect 给角色授权用，三处数据源不同但组装规则一致，集中维护避免出现“某个入口少一层”的 bug。</p>
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final String STATUS_ENABLED = "0";
    private static final String VISIBLE_SHOW = "0";

    private final SysMenuMapper menuMapper;
    private final SysMenuCustomMapper menuCustomMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserCustomMapper userCustomMapper;

    @Override
    public List<SysMenu> list(String menuName, String status) {
        LambdaQueryWrapper<SysMenu> wrapper = Wrappers.<SysMenu>lambdaQuery()
                .like(StrUtil.isNotBlank(menuName), SysMenu::getMenuName, menuName)
                .eq(StrUtil.isNotBlank(status), SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum);
        return menuMapper.selectList(wrapper);
    }

    @Override
    public SysMenu detail(Long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);
        ServiceException.throwIf(menu == null, "菜单不存在");
        return menu;
    }

    @Override
    public void add(SysMenu menu) {
        ServiceException.throwIf(StrUtil.isBlank(menu.getMenuName()), "菜单名称不能为空");
        ServiceException.throwIf(StrUtil.isBlank(menu.getMenuType()), "菜单类型不能为空");
        if (menu.getParentId() == null) {
            menu.setParentId(ROOT_PARENT_ID);
        }
        if (menu.getOrderNum() == null) {
            menu.setOrderNum(0);
        }
        menu.setStatus(StrUtil.blankToDefault(menu.getStatus(), STATUS_ENABLED));
        menu.setVisible(StrUtil.blankToDefault(menu.getVisible(), VISIBLE_SHOW));
        menuMapper.insert(menu);
    }

    @Override
    public void update(SysMenu menu) {
        ServiceException.throwIf(menu.getMenuId() == null, "菜单ID不能为空");
        ServiceException.throwIf(menuMapper.selectById(menu.getMenuId()) == null, "菜单不存在");
        // 不允许把菜单挂到自己身上，否则树构建时会形成自环
        ServiceException.throwIf(menu.getMenuId().equals(menu.getParentId()), "上级菜单不能选择自己");
        menuMapper.updateById(menu);
    }

    @Override
    public void remove(Long menuId) {
        ServiceException.throwIf(menuMapper.selectById(menuId) == null, "菜单不存在");
        long children = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getParentId, menuId));
        ServiceException.throwIf(children > 0, "存在子菜单，不允许删除");
        ServiceException.throwIf(roleMenuMapper.countByMenuId(menuId) > 0, "菜单已分配角色，不允许删除");
        // sys_menu 没有逻辑删除字段，deleteById 是物理删除
        menuMapper.deleteById(menuId);
    }

    @Override
    public List<TreeSelectVO> treeSelect() {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        Map<Long, TreeSelectVO> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            TreeSelectVO node = new TreeSelectVO();
            node.setId(menu.getMenuId());
            node.setLabel(menu.getMenuName());
            nodeMap.put(menu.getMenuId(), node);
        }
        List<TreeSelectVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            TreeSelectVO node = nodeMap.get(menu.getMenuId());
            TreeSelectVO parent = nodeMap.get(menu.getParentId());
            if (parent == null || Objects.equals(parent.getId(), node.getId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortTreeSelect(roots);
        return roots;
    }

    @Override
    public RoleMenuTreeVO roleMenuTree(Long roleId) {
        RoleMenuTreeVO vo = new RoleMenuTreeVO();
        vo.setMenus(treeSelect());
        vo.setCheckedKeys(roleMenuMapper.selectMenuIdsByRoleId(roleId));
        return vo;
    }

    @Override
    public List<MenuNodeDTO> routers() {
        return userMenuTree(SecurityUtils.requireUserId());
    }

    @Override
    public List<MenuNodeDTO> allTree() {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        return buildMenuTree(menus, false);
    }

    @Override
    public List<MenuNodeDTO> userMenuTree(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        // 超级管理员返回全部，否则按角色授权查；这层判断与 auth 的登录信息保持一致
        List<String> roles = userCustomMapper.selectRoleKeysByUserId(userId);
        boolean admin = roles != null && roles.contains("admin");
        List<SysMenu> menus = admin ? menuCustomMapper.selectAllMenuTree()
                : menuCustomMapper.selectUserMenuTree(userId);
        return buildMenuTree(menus, true);
    }

    /* ---------------- 树构建 ---------------- */

    /**
     * 构建菜单树。
     *
     * <p>只按「数据库里查到的行」自底向上挂 parent，查不到父节点的行提升为根节点：
     * 普通用户可能只被授予了子菜单而没授予父目录，若不提升就会出现“有权限但看不到菜单”。</p>
     */
    private List<MenuNodeDTO> buildMenuTree(List<SysMenu> menus, boolean onlyVisible) {
        List<MenuNodeDTO> roots = new ArrayList<>();
        if (menus == null || menus.isEmpty()) {
            return roots;
        }
        Map<Long, MenuNodeDTO> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            if (onlyVisible && (!STATUS_ENABLED.equals(menu.getStatus())
                    || !VISIBLE_SHOW.equals(menu.getVisible()))) {
                continue;
            }
            nodeMap.put(menu.getMenuId(), toNode(menu));
        }
        for (SysMenu menu : menus) {
            MenuNodeDTO node = nodeMap.get(menu.getMenuId());
            if (node == null) {
                continue;
            }
            MenuNodeDTO parent = menu.getParentId() == null ? null : nodeMap.get(menu.getParentId());
            if (parent == null || Objects.equals(parent.getMenuId(), node.getMenuId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortNodes(roots);
        return roots;
    }

    private MenuNodeDTO toNode(SysMenu menu) {
        MenuNodeDTO node = new MenuNodeDTO();
        node.setMenuId(menu.getMenuId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setQueryParam(menu.getQueryParam());
        node.setMenuType(menu.getMenuType());
        node.setPerms(menu.getPerms());
        node.setIcon(menu.getIcon());
        node.setOrderNum(menu.getOrderNum());
        node.setVisible(menu.getVisible());
        node.setIsFrame(menu.getIsFrame());
        node.setIsCache(menu.getIsCache());
        node.setChildren(new ArrayList<>());
        return node;
    }

    private void sortNodes(List<MenuNodeDTO> nodes) {
        nodes.sort(Comparator.comparing((MenuNodeDTO n) -> n.getOrderNum() == null ? 0 : n.getOrderNum())
                .thenComparing(MenuNodeDTO::getMenuId));
        nodes.forEach(node -> sortNodes(node.getChildren()));
    }

    private void sortTreeSelect(List<TreeSelectVO> nodes) {
        // TreeSelectVO 没有 orderNum，菜单查询已按 parent_id/order_num 排序，这里只需递归保持原顺序即可
        nodes.forEach(node -> sortTreeSelect(node.getChildren()));
    }
}
