package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 最新成交动态
 *
 * @author campus-mart
 */
@Data
@Schema(description = "最新成交动态")
public class RecentOrderDTO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "成交金额")
    private BigDecimal amount;

    @Schema(description = "买家昵称")
    private String buyerNickname;

    @Schema(description = "卖家昵称")
    private String sellerNickname;

    @Schema(description = "成交时间")
    private LocalDateTime finishTime;
}
