package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 全局累计统计行（总览卡片使用）
 *
 * @author campus-mart
 */
@Data
public class OverviewTotalsRow {

    /** 累计成交额 */
    private BigDecimal totalGmv;

    /** 累计下单数 */
    private Long totalOrderCount;

    /** 累计成交订单数 */
    private Long totalFinishOrderCount;

    /** 累计学生用户数 */
    private Long totalUserCount;

    /** 累计商品数 */
    private Long totalItemCount;

    /** 在售商品数 */
    private Long onSaleItemCount;

    /** 待审核商品数 */
    private Long pendingAuditItemCount;

    /** 累计收藏数 */
    private Long totalFavoriteCount;

    /** 累计浏览数 */
    private Long totalViewCount;

    /** 累计 AI 调用次数 */
    private Long totalAiCallCount;
}
