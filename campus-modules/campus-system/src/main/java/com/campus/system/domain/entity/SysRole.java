package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_role 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_role")
public class SysRole {

    /** 角色ID */
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识，如 admin/operator */
    private String roleKey;

    /** 排序 */
    private Integer roleSort;

    /** 数据范围 1全部 2本学院 3本人 */
    private String dataScope;

    /** 状态 0正常 1停用 */
    private String status;

    /**  */
    private String remark;

    /**  */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
