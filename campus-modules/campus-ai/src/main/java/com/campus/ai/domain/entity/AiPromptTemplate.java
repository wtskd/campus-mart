package com.campus.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ai_prompt_template 实体
 *
 * @author campus-mart
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    /**  */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模板编码，如 ITEM_COPYWRITE */
    private String code;

    /**  */
    private String name;

    /** 场景 copywrite/chat/audit/price */
    private String scene;

    /**  */
    private String systemPrompt;

    /** 支持 {{变量}} 占位 */
    private String userPrompt;

    /** 为空用默认模型 */
    private String model;

    /**  */
    private BigDecimal temperature;

    /**  */
    private Integer maxTokens;

    /** 0启用 1停用 */
    private String status;

    /**  */
    private String remark;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
