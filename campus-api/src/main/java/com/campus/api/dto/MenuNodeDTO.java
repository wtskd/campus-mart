package com.campus.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态菜单节点
 *
 * @author campus-mart
 */
@Data
@Schema(description = "菜单节点")
public class MenuNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long menuId;

    private Long parentId;

    private String menuName;

    /** 路由路径 */
    private String path;

    /** 前端组件路径，如 system/user/index */
    private String component;

    /** 路由参数 */
    private String queryParam;

    /** 菜单类型 M目录 C菜单 F按钮 */
    private String menuType;

    /** 权限标识 */
    private String perms;

    private String icon;

    private Integer orderNum;

    /** 是否显示 0显示 1隐藏 */
    private String visible;

    /** 是否外链 0否 1是 */
    private String isFrame;

    /** 是否缓存 0缓存 1不缓存 */
    private String isCache;

    private List<MenuNodeDTO> children = new ArrayList<>();
}
