package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 趋势图数据点
 *
 * @author campus-mart
 */
@Data
@Schema(description = "趋势图数据点")
public class TrendPointDTO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "成交额")
    private BigDecimal gmv;

    @Schema(description = "下单数")
    private Long orderCount;

    @Schema(description = "成交订单数")
    private Long finishOrderCount;

    @Schema(description = "新增用户数")
    private Long newUserCount;

    @Schema(description = "新增商品数")
    private Long newItemCount;

    @Schema(description = "AI 调用次数")
    private Long aiCallCount;
}
