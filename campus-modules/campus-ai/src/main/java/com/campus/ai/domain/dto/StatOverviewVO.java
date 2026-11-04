package com.campus.ai.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 使用概览
 *
 * @author campus-mart
 */
@Data
public class StatOverviewVO {

    /** 统计天数（含今天） */
    private Integer days;

    /** 统计起始时间 */
    private String beginTime;

    private Long totalCalls = 0L;

    private Long successCalls = 0L;

    /** 降级兜底次数（remote/local 失败后 mock 成功） */
    private Long fallbackCalls = 0L;

    private Long failCalls = 0L;

    /** 成功率（百分比，保留两位小数） */
    private Double successRate = 0D;

    private Double avgCostMs = 0D;

    private Long promptTokens = 0L;

    private Long completionTokens = 0L;

    private Long totalTokens = 0L;

    /** token 估算值：历史数据没有 token 时按调用次数估算 */
    private Long estimatedTokens = 0L;

    private Boolean tokenEstimated = false;

    private List<SceneStatVO> scenes = new ArrayList<>();

    private List<ProviderStatVO> providers = new ArrayList<>();

    /** 契约字段：按场景分布 [{scene, count}] */
    private List<SceneCountVO> byScene = new ArrayList<>();

    /** 契约字段：按天分布 [{date, count}] */
    private List<DayCountVO> byDay = new ArrayList<>();

    @Data
    public static class SceneCountVO {
        private String scene;
        private Long count = 0L;
    }

    @Data
    public static class DayCountVO {
        private String date;
        private Long count = 0L;
    }

    @Data
    public static class SceneStatVO {
        private String scene;
        private String sceneName;
        private Long calls = 0L;
        private Long successCalls = 0L;
        private Long fallbackCalls = 0L;
        private Long failCalls = 0L;
        private Double avgCostMs = 0D;
        private Long totalTokens = 0L;
        /** 占比（百分比） */
        private Double ratio = 0D;
    }

    @Data
    public static class ProviderStatVO {
        private String provider;
        private Long calls = 0L;
        private Double ratio = 0D;
    }
}
