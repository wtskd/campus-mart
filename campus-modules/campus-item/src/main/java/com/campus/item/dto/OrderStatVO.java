package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 后台订单顶部统计
 *
 * @author campus-mart
 */
@Data
@Schema(description = "后台订单顶部统计")
public class OrderStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日订单数")
    private long todayOrderCount;

    @Schema(description = "今日成交额（已完成订单）")
    private BigDecimal todayTradeAmount;

    @Schema(description = "待处理订单数（待确认 + 待交付）")
    private long pendingCount;
}
