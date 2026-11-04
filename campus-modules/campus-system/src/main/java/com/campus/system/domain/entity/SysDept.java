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
 * sys_dept 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_dept")
public class SysDept {

    /** 学院ID */
    @TableId(value = "dept_id", type = IdType.AUTO)
    private Long deptId;

    /** 父ID，0为根 */
    private Long parentId;

    /** 祖级列表，逗号分隔 */
    private String ancestors;

    /** 学院/班级名称 */
    private String deptName;

    /** 排序 */
    private Integer orderNum;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 状态 0正常 1停用 */
    private String status;

    /** 删除标记 0存在 1删除 */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
