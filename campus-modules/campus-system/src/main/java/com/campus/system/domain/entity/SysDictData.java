package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_dict_data 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_dict_data")
public class SysDictData {

    /**  */
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Long dictCode;

    /** 字典类型编码 */
    private String dictType;

    /** 显示值 */
    private String dictLabel;

    /** 实际值 */
    private String dictValue;

    /**  */
    private Integer dictSort;

    /** 样式类 */
    private String cssClass;

    /** 标签样式 */
    private String listClass;

    /**  */
    private String isDefault;

    /**  */
    private String status;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
