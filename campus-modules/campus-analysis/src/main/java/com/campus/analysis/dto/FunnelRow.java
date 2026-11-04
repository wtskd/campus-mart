package com.campus.analysis.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 漏斗统计行
 *
 * <p>口径：曝光 = 统计区间内发布商品的 view_count 汇总；收藏 = biz_favorite 行数；
 * 下单 = biz_order 行数；支付 = pay_time 非空；成交 = status = 2。</p>
 *
 * @author campus-mart
 */
@Data
public class FunnelRow {

    /** 统计日期（按天展示时使用；区间汇总时为 null） */
    private LocalDate statDate;

    /** 商品曝光数 */
    private Long viewCount;

    /** 收藏数 */
    private Long favoriteCount;

    /** 下单数 */
    private Long orderCount;

    /** 支付数 */
    private Long payCount;

    /** 成交数 */
    private Long finishCount;
}
