package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色菜单授权入参：{ "roleId":"2", "menuIds":["1","100"] }
 *
 * @author campus-mart
 */
@Data
@Schema(description = "角色菜单授权参数")
public class RoleMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    @Schema(description = "勾选的菜单ID集合，空集合表示清空授权")
    private List<Long> menuIds;
}
