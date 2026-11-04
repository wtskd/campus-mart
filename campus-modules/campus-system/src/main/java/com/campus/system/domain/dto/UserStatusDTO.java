package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户启停入参：{ "userId":"11", "status":"0" }
 *
 * @author campus-mart
 */
@Data
@Schema(description = "用户状态修改参数")
public class UserStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    @Schema(description = "状态 0正常 1停用")
    private String status;
}
