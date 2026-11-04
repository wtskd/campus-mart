package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台订单分页查询参数
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台订单分页查询参数")
public class OrderQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号（模糊匹配）")
    private String orderNo;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "订单状态：0待确认 1待交付 2已完成 3已取消 4退款中 5已退款")
    private Integer status;

    @Schema(description = "下单时间起，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "下单时间止，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
