package com.campus.system.mapper;

import com.campus.system.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单相关的多表查询：动态路由与权限标识都来自「用户 -> 角色 -> 菜单」三张表。
 *
 * @author campus-mart
 */
public interface SysMenuCustomMapper {

    /** 某用户可见的目录+菜单（visible='0' 且启用），parent_id/order_num 升序方便前端顺序建树 */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.status = '0' AND r.del_flag = '0'
            WHERE ur.user_id = #{userId}
              AND m.menu_type IN ('M', 'C')
              AND m.visible = '0'
              AND m.status = '0'
            ORDER BY m.parent_id, m.order_num
            """)
    List<SysMenu> selectUserMenuTree(@Param("userId") Long userId);

    /** 全量目录+菜单（超级管理员不受角色限制） */
    @Select("""
            SELECT m.* FROM sys_menu m
            WHERE m.menu_type IN ('M', 'C') AND m.visible = '0' AND m.status = '0'
            ORDER BY m.parent_id, m.order_num
            """)
    List<SysMenu> selectAllMenuTree();

    /** 用户拥有的菜单ID（含按钮，auth 用它做动态菜单校验） */
    @Select("""
            SELECT DISTINCT rm.menu_id
            FROM sys_role_menu rm
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.status = '0' AND r.del_flag = '0'
            WHERE ur.user_id = #{userId}
            """)
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_menu")
    List<Long> selectAllMenuIds();

    /** 用户拥有的权限标识（去重、忽略空值），供 auth 写进 LoginUser.perms */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.status = '0' AND r.del_flag = '0'
            WHERE ur.user_id = #{userId}
              AND m.status = '0'
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
