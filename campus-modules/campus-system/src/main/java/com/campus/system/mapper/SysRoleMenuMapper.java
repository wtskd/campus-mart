package com.campus.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * sys_role_menu 中间表操作：角色授权即“整体重写”这组关系。
 *
 * @author campus-mart
 */
public interface SysRoleMenuMapper {

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 批量插入授权关系；menuIds 为空时调用方必须提前拦截，否则 SQL 语法错误 */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu(role_id, menu_id) VALUES
            <foreach collection='menuIds' item='mid' separator=','>(#{roleId}, #{mid})</foreach>
            </script>
            """)
    int insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /** 菜单是否被角色引用——删除菜单前的校验 */
    @Select("SELECT COUNT(1) FROM sys_role_menu WHERE menu_id = #{menuId}")
    long countByMenuId(@Param("menuId") Long menuId);
}
