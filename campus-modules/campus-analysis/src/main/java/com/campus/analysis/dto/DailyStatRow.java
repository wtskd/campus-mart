package com.campus.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 按天聚合的原始统计行（由自定义 SQL 直接聚合，对应 stat_overview_daily 的字段结构）
 *
 * @author campus-mart
 */
@Data
public class DailyStatRow {

    /** 统计日期 */
    private LocalDate statDate;

    /** 成交额（status=2 的订单金额） */
    private BigDecimal gmv;

    /** 下单数（按 create_time） */
    private Long orderCount;

    /** 成交订单数（status=2） */
    private Long finishOrderCount;

    /** 取消/退款订单数（status in 3,4,5） */
    private Long cancelOrderCount;

    /** 商品发布数 */
    private Long itemPublishCount;

    /** 成交商品数（= 成交订单数） */
    private Long itemSoldCount;

    /** 新增学生用户数 */
    private Long newUserCount;

    /** 活跃用户数（当日有下单或收藏去重用户数） */
    private Long activeUserCount;

    /** 商品浏览数（当日发布商品的累计 view_count） */
    private Long viewCount;

    /** 收藏数 */
    private Long favoriteCount;

    /** AI 调用次数 */
    private Long aiCallCount;

    /** 是否全天无任何数据（用于跳过空数据日写库） */
    public boolean isEmptyDay() {
        return zero(gmv) && zero(orderCount) && zero(finishOrderCount) && zero(itemPublishCount)
                && zero(newUserCount) && zero(favoriteCount) && zero(aiCallCount) && zero(viewCount);
    }

    private static boolean zero(Number n) {
        return n == null || n.doubleValue() == 0D;
    }
}
