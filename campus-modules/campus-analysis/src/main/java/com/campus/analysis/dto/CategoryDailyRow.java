package com.campus.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 分类成交按天统计行
 *
 * @author campus-mart
 */
@Data
public class CategoryDailyRow {

    /** 统计日期 */
    private LocalDate statDate;

    /** 一级分类 ID */
    private Long categoryId;

    /** 成交订单数 */
    private Long orderCount;

    /** 成交金额 */
    private BigDecimal gmv;
}
