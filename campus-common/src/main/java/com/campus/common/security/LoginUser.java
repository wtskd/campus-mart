package com.campus.common.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文
 *
 * @author campus-mart
 */
@Data
@Schema(description = "登录用户信息")
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    /** 头像地址（/upload/demo/avatar-01.png 这类相对路径） */
    private String avatar;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "角色标识集合")
    private List<String> roles;

    @Schema(description = "权限标识集合")
    private Set<String> perms = new HashSet<>();

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "令牌过期时间戳(毫秒)")
    private Long expireAt;

    /** 是否超级管理员 */
    public boolean isAdmin() {
        return perms != null && (perms.contains("*:*:*") || perms.contains("admin"));
    }
}
