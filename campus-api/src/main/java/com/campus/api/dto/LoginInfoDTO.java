package com.campus.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录信息（auth 服务调用 system 服务获取）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "登录所需用户信息")
public class LoginInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    /** 加密后的密码（仅内部校验用，不下发前端） */
    private String password;

    private Long deptId;

    private String deptName;

    private String avatar;

    /** 账号状态 0 正常 1 停用 */
    private String status;

    /** 删除标志 0 存在 2 删除 */
    private String delFlag;

    private List<String> roles;

    private List<String> perms;
}
