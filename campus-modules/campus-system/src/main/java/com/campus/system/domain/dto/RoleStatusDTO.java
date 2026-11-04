package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色启停入参：{ "roleId":"2", "status":"0" }
 *
 * @author campus-mart
 */
@Data
@Schema(description = "角色状态修改参数")
public class RoleStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    @Schema(description = "状态 0正常 1停用")
    private String status;
}
