package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 数据概览 DTO
 *
 * @author campus-mart
 */
@Data
@Schema(description = "数据概览")
public class OverviewDTO {

    @Schema(description = "统计日期 yyyy-MM-dd")
    private String statDate;

    @Schema(description = "今日成交额")
    private BigDecimal todayGmv;

    @Schema(description = "今日下单数")
    private Long todayOrderCount;

    @Schema(description = "今日成交订单数")
    private Long todayFinishOrderCount;

    @Schema(description = "今日新增用户数")
    private Long todayNewUserCount;

    @Schema(description = "今日新增商品数")
    private Long todayNewItemCount;

    @Schema(description = "今日 AI 调用次数")
    private Long todayAiCallCount;

    @Schema(description = "今日平均客单价")
    private BigDecimal todayAvgOrderAmount;

    @Schema(description = "昨日成交额")
    private BigDecimal yesterdayGmv;

    @Schema(description = "昨日下单数")
    private Long yesterdayOrderCount;

    @Schema(description = "昨日成交订单数")
    private Long yesterdayFinishOrderCount;

    @Schema(description = "昨日新增用户数")
    private Long yesterdayNewUserCount;

    @Schema(description = "昨日新增商品数")
    private Long yesterdayNewItemCount;

    @Schema(description = "昨日 AI 调用次数")
    private Long yesterdayAiCallCount;

    @Schema(description = "昨日平均客单价")
    private BigDecimal yesterdayAvgOrderAmount;

    @Schema(description = "近 7 日成交额")
    private BigDecimal weekGmv;

    @Schema(description = "近 7 日下单数")
    private Long weekOrderCount;

    @Schema(description = "近 7 日成交订单数")
    private Long weekFinishOrderCount;

    @Schema(description = "近 7 日新增用户数")
    private Long weekNewUserCount;

    @Schema(description = "近 7 日新增商品数")
    private Long weekNewItemCount;

    @Schema(description = "近 7 日 AI 调用次数")
    private Long weekAiCallCount;

    @Schema(description = "近 7 日平均客单价")
    private BigDecimal weekAvgOrderAmount;

    @Schema(description = "累计成交额")
    private BigDecimal totalGmv;

    @Schema(description = "累计下单数")
    private Long totalOrderCount;

    @Schema(description = "累计成交订单数")
    private Long totalFinishOrderCount;

    @Schema(description = "累计学生用户数")
    private Long totalUserCount;

    @Schema(description = "累计商品数")
    private Long totalItemCount;

    @Schema(description = "累计 AI 调用次数")
    private Long totalAiCallCount;

    @Schema(description = "累计收藏数")
    private Long totalFavoriteCount;

    @Schema(description = "累计浏览数")
    private Long totalViewCount;

    @Schema(description = "在售商品数")
    private Long onSaleItemCount;

    @Schema(description = "待审核商品数")
    private Long pendingAuditItemCount;

    @Schema(description = "平均客单价（累计成交额 / 累计成交订单数）")
    private BigDecimal avgOrderAmount;

    @Schema(description = "今日成交额环比增长率（相比昨日，0.1234 = 12.34%）")
    private Double gmvCompare;

    @Schema(description = "今日下单数环比增长率（相比昨日，0.1234 = 12.34%）")
    private Double orderCompare;
}
