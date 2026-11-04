package com.campus.system.mapper;

import com.campus.system.domain.dto.UserRoleRef;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * sys_user_role 中间表操作。
 *
 * <p>约定见 docs/05-backend-conventions.md：中间表不生成实体，统一用自定义 SQL 维护，
 * 这样用户-角色关系的变化不会受 MyBatis-Plus 逻辑删除影响（中间表是物理删除）。</p>
 *
 * @author campus-mart
 */
public interface SysUserRoleMapper {

    /** 查询用户已分配的角色ID */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 批量查询（用户列表页一次性回填 roleIds，避免 N+1 查询） */
    @Select("""
            <script>
            SELECT user_id AS userId, role_id AS roleId FROM sys_user_role
            WHERE user_id IN
            <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<UserRoleRef> selectByUserIds(@Param("userIds") List<Long> userIds);

    /** 分配单个角色；重复分配直接忽略，保证重试幂等 */
    @Insert("INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertOne(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 用户被逻辑删除时同步清理关系行，避免“已删除用户占着角色”导致角色无法删除 */
    @Delete("""
            <script>
            DELETE FROM sys_user_role WHERE user_id IN
            <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    int deleteByUserIds(@Param("userIds") List<Long> userIds);

    /** 角色是否已分配用户——删除角色前的校验 */
    @Select("SELECT COUNT(1) FROM sys_user_role WHERE role_id = #{roleId}")
    long countByRoleId(@Param("roleId") Long roleId);
}
