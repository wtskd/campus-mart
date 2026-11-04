package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_menu 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 菜单ID */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单ID */
    private Long parentId;

    /** 排序 */
    private Integer orderNum;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 路由参数 */
    private String queryParam;

    /** 是否外链 0是 1否 */
    private String isFrame;

    /** 是否缓存 0缓存 1不缓存 */
    private String isCache;

    /** 类型 M目录 C菜单 F按钮 */
    private String menuType;

    /** 显示 0显示 1隐藏 */
    private String visible;

    /** 状态 0正常 1停用 */
    private String status;

    /** 权限标识 */
    private String perms;

    /** 图标 */
    private String icon;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
