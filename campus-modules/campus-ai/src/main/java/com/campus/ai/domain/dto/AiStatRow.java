package com.campus.ai.domain.dto;

import lombok.Data;

/**
 * 统计 SQL 行（MyBatis 自动映射下划线转驼峰）
 *
 * @author campus-mart
 */
public final class AiStatRow {

    private AiStatRow() {
    }

    @Data
    public static class Total {
        private Long totalCalls;
        private Long successCalls;
        private Long fallbackCalls;
        private Long failCalls;
        private Double avgCostMs;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
    }

    @Data
    public static class Scene {
        private String scene;
        private Long calls;
        private Long successCalls;
        private Long fallbackCalls;
        private Long failCalls;
        private Double avgCostMs;
        private Long totalTokens;
    }

    @Data
    public static class Provider {
        private String provider;
        private Long calls;
    }

    @Data
    public static class Day {
        private String statDate;
        private Long calls;
    }
}
