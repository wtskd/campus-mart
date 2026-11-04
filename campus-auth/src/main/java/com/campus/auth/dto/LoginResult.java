package com.campus.auth.dto;

import com.campus.common.security.LoginUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应
 *
 * @author campus-mart
 */
@Data
@AllArgsConstructor
@Schema(description = "登录结果")
public class LoginResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "有效期(秒)")
    private long expiresIn;

    @Schema(description = "用户信息")
    private LoginUser userInfo;
}
