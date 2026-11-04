package com.campus.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色菜单授权页数据：{ "menus":[树], "checkedKeys":[已选菜单ID] }
 *
 * <p>一次请求同时返回「可选树」与「已勾选」，避免前端先拉树再拉已选造成两次往返。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "角色菜单授权树")
public class RoleMenuTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "全部菜单树")
    private List<TreeSelectVO> menus;

    @Schema(description = "该角色已勾选的菜单ID")
    private List<Long> checkedKeys;
}
