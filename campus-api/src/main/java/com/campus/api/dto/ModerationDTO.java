package com.campus.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 内容审核结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "内容审核结果")
public class ModerationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过 */
    private boolean pass;

    /** 风险等级 0 安全 1 可疑 2 违规 */
    private int riskLevel;

    /** 命中的敏感词 */
    private List<String> words;

    /** 审核说明 */
    private String reason;

    /** 实际使用的 AI 提供方 remote/local/rule */
    private String provider;

    /** 耗时(毫秒) */
    private Long costTime;
}
