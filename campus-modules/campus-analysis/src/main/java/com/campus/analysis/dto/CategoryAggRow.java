package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 分类区间汇总行（读取 stat_category_daily 或实时聚合共用）
 *
 * @author campus-mart
 */
@Data
public class CategoryAggRow {

    /** 一级分类 ID */
    private Long categoryId;

    /** 一级分类名称 */
    private String categoryName;

    /** 成交订单数 */
    private Long orderCount;

    /** 成交金额 */
    private BigDecimal gmv;
}
