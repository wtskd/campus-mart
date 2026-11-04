package com.campus.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ai_call_log 实体
 *
 * @author campus-mart
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    /**  */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**  */
    private Long userId;

    /** 场景 */
    private String scene;

    /**  */
    private String templateCode;

    /** remote/local */
    private String provider;

    /**  */
    private String model;

    /** 请求摘要（脱敏截断） */
    private String requestSummary;

    /**  */
    private Integer promptTokens;

    /**  */
    private Integer completionTokens;

    /**  */
    private Integer totalTokens;

    /**  */
    private Long costMs;

    /** 0成功 1失败 2降级兜底 */
    private String status;

    /**  */
    private String errorMsg;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
