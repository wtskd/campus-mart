package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 场景使用统计
 *
 * @author campus-mart
 */
@Data
@Schema(description = "AI 场景使用统计")
public class AiSceneStatDTO {

    @Schema(description = "场景标识")
    private String scene;

    @Schema(description = "调用次数")
    private Long callCount;

    @Schema(description = "成功次数")
    private Long successCount;

    @Schema(description = "降级次数")
    private Long fallbackCount;

    @Schema(description = "失败次数")
    private Long failCount;

    @Schema(description = "成功率（0-1）")
    private Double successRate;

    @Schema(description = "平均耗时（毫秒）")
    private Double avgCostMs;

    @Schema(description = "token 总消耗")
    private Long totalTokens;

    @Schema(description = "单次平均 token 消耗")
    private Double avgTokens;
}
