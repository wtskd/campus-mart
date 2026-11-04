package com.campus.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ai_knowledge 实体
 *
 * @author campus-mart
 */
@Data
@TableName("ai_knowledge")
public class AiKnowledge {

    /**  */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** faq/rule/category */
    private String kbType;

    /**  */
    private String title;

    /**  */
    private String content;

    /**  */
    private String keywords;

    /**  */
    private String status;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
