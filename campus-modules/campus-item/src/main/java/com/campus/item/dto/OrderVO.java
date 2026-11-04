package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 *
 * @author campus-mart
 */
@Data
@Schema(description = "订单信息")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "商品封面")
    private String itemCover;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "买家昵称")
    private String buyerNickname;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "卖家昵称")
    private String sellerNickname;

    @Schema(description = "成交金额")
    private BigDecimal amount;

    @Schema(description = "订单状态：0待确认 1待交付 2已完成 3已取消 4退款中 5已退款")
    private Integer status;

    @Schema(description = "订单状态文案")
    private String statusText;

    @Schema(description = "支付方式：0线下 1模拟在线")
    private Integer payType;

    @Schema(description = "交易地点")
    private String tradePlace;

    @Schema(description = "买家留言")
    private String buyerRemark;

    @Schema(description = "取消/退款原因")
    private String cancelReason;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "付款时间")
    private LocalDateTime payTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "是否已评价")
    private Boolean commented;

    @Schema(description = "评价ID")
    private Long commentId;

    @Schema(description = "评分 1-5")
    private Integer commentScore;

    @Schema(description = "评价内容")
    private String commentContent;

    @Schema(description = "评价时间")
    private LocalDateTime commentTime;

    @Schema(description = "卖家回复")
    private String commentReply;
}
