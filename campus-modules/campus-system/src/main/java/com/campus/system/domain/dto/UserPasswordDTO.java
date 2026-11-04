package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码入参：{ "userId":"11", "password":"123456" }
 *
 * @author campus-mart
 */
@Data
@Schema(description = "重置密码参数")
public class UserPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    @Schema(description = "新密码（明文，服务端 BCrypt 加密后入库）")
    private String password;
}
