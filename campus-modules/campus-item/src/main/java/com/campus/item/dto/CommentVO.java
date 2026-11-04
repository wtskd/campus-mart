package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价列表项
 *
 * @author campus-mart
 */
@Data
@Schema(description = "评价列表项")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID")
    private Long commentId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "评价人ID")
    private Long fromUserId;

    @Schema(description = "评价人昵称")
    private String fromNickname;

    @Schema(description = "评价人头像")
    private String fromAvatar;

    @Schema(description = "被评价人ID")
    private Long toUserId;

    @Schema(description = "被评价人昵称")
    private String toNickname;

    @Schema(description = "评分 1-5")
    private Integer score;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价图片")
    private List<String> images;

    @Schema(description = "卖家回复")
    private String replyContent;

    @Schema(description = "状态 0正常 1隐藏 2审核未通过")
    private String status;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}
