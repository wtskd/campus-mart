package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 发表评价结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "发表评价结果")
public class CommentCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID")
    private Long commentId;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "状态 0正常 1隐藏 2审核未通过")
    private String status;

    @Schema(description = "提示信息")
    private String message;
}
