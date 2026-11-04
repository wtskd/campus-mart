package com.campus.ai.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 智能估价响应
 *
 * @author campus-mart
 */
@Data
public class PriceResponse {

    /** 建议售价 */
    private BigDecimal suggestPrice;

    /** 建议最低价 */
    private BigDecimal minPrice;

    /** 建议最高价 */
    private BigDecimal maxPrice;

    /** 中文估价说明 */
    private String reason;

    /** 实际使用的 provider：remote / local / mock */
    private String provider;

    /** 耗时（毫秒） */
    private Long costMs;
}
