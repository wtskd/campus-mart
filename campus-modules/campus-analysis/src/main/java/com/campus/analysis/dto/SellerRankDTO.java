package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 卖家排行
 *
 * @author campus-mart
 */
@Data
@Schema(description = "卖家排行")
public class SellerRankDTO {

    @Schema(description = "卖家用户 ID")
    private Long sellerId;

    @Schema(description = "卖家昵称")
    private String nickname;

    @Schema(description = "院系名称")
    private String deptName;

    @Schema(description = "成交金额")
    private BigDecimal amount;

    @Schema(description = "成交单数")
    private Long orderCount;

    @Schema(description = "收到评价数")
    private Long commentCount;

    @Schema(description = "好评率（score >= 4，0-1）")
    private Double goodRate;
}
