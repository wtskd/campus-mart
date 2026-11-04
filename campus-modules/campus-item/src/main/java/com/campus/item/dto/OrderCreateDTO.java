package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单请求
 *
 * <p>二手单品交易：一个商品同一时间只能有一条进行中的订单，不支持购物车。</p>
 *
 * @author campus-mart
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long itemId;

    @Schema(description = "交易地点，不填则默认取商品所在地")
    @Size(max = 64, message = "交易地点不能超过 64 个字符")
    private String tradePlace;

    @Schema(description = "买家留言")
    @Size(max = 255, message = "买家留言不能超过 255 个字符")
    private String buyerRemark;
}
