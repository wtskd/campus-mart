package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台人工变更订单状态请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "后台人工变更订单状态请求")
public class OrderStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "目标状态：0待确认 1待交付 2已完成 3已取消 4退款中 5已退款",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单状态不能为空")
    @Min(value = 0, message = "订单状态不合法")
    @Max(value = 5, message = "订单状态不合法")
    private Integer status;

    @Schema(description = "备注，取消/退款时写入取消原因")
    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String remark;
}
