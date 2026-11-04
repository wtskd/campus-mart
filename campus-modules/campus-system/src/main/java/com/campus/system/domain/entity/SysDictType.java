package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_dict_type 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {

    /**  */
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Long dictId;

    /** 字典名称 */
    private String dictName;

    /** 字典类型编码 */
    private String dictType;

    /**  */
    private String status;

    /**  */
    private String remark;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
