package com.campus.system.mapper;

import com.campus.api.dto.LoginInfoDTO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户相关的复杂/特殊查询（生成器只生成了 BaseMapper，这里补充）。
 *
 * @author campus-mart
 */
public interface SysUserCustomMapper {

    /**
     * 登录信息（含 deptName）。
     *
     * <p>故意使用原生 SQL：生成实体上的 {@code @TableLogic} 会把 del_flag='2' 的记录过滤掉，
     * 而 auth 需要拿到 delFlag 才能给出「账号不存在」而不是「用户名或密码错误」的精确提示。</p>
     */
    @Select("""
            SELECT u.user_id, u.username, u.nickname, u.password, u.dept_id, d.dept_name,
                   u.avatar, u.status, u.del_flag
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.dept_id = u.dept_id
            WHERE u.username = #{username}
            LIMIT 1
            """)
    LoginInfoDTO selectLoginInfoByUsername(@Param("username") String username);

    @Select("""
            SELECT u.user_id, u.username, u.nickname, u.password, u.dept_id, d.dept_name,
                   u.avatar, u.status, u.del_flag
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.dept_id = u.dept_id
            WHERE u.user_id = #{userId}
            LIMIT 1
            """)
    LoginInfoDTO selectLoginInfoByUserId(@Param("userId") Long userId);

    /** 用户拥有的角色标识（只取启用状态的角色，停用角色等于没有权限） */
    @Select("""
            SELECT r.role_key
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId} AND r.status = '0' AND r.del_flag = '0'
            ORDER BY r.role_sort
            """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 按用户名查 ID（含已逻辑删除的记录）。
     *
     * <p>username 上有唯一索引，逻辑删除的行仍占用索引值，所以新增校验必须连已删除的行一起查，
     * 否则会出现“提示唯一但插入报 Duplicate entry”的诡异错误。</p>
     */
    @Select("SELECT user_id FROM sys_user WHERE username = #{username}")
    List<Long> selectUserIdsByUsername(@Param("username") String username);
}
