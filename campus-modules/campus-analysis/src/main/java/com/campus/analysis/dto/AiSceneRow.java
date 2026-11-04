package com.campus.analysis.dto;

import lombok.Data;

/**
 * AI 调用场景统计行
 *
 * @author campus-mart
 */
@Data
public class AiSceneRow {

    /** 场景标识 */
    private String scene;

    /** 调用次数 */
    private Long callCount;

    /** 成功次数 */
    private Long successCount;

    /** 降级次数 */
    private Long fallbackCount;

    /** 失败次数 */
    private Long failCount;

    /** 平均耗时（毫秒） */
    private Double avgCostMs;

    /** token 总消耗 */
    private Long totalTokens;
}
