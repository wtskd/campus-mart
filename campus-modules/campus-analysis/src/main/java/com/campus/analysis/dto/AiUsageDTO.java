package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * AI 使用情况
 *
 * @author campus-mart
 */
@Data
@Schema(description = "AI 使用情况")
public class AiUsageDTO {

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "调用总次数")
    private Long totalCallCount;

    @Schema(description = "成功总次数")
    private Long successCount;

    @Schema(description = "降级总次数")
    private Long fallbackCount;

    @Schema(description = "失败总次数")
    private Long failCount;

    @Schema(description = "整体成功率（0-1）")
    private Double successRate;

    @Schema(description = "整体平均耗时（毫秒）")
    private Double avgCostMs;

    @Schema(description = "token 总消耗")
    private Long totalTokens;

    @Schema(description = "按场景统计")
    private List<AiSceneStatDTO> scenes = new ArrayList<>();
}
