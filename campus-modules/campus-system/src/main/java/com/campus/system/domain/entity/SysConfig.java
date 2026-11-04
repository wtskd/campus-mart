package com.campus.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_config 实体
 *
 * @author campus-mart
 */
@Data
@TableName("sys_config")
public class SysConfig {

    /**  */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /** 参数名称 */
    private String configName;

    /** 参数键 */
    private String configKey;

    /** 参数值 */
    private String configValue;

    /** 是否内置 Y/N */
    private String configType;

    /**  */
    private String remark;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
