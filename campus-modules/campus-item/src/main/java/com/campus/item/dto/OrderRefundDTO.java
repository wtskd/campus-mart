package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台订单退款请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "订单退款请求")
public class OrderRefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @Size(max = 255, message = "退款原因不能超过 255 个字符")
    @Schema(description = "退款原因")
    private String reason;
}
