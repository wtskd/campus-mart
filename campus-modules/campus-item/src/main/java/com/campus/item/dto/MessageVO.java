package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息视图对象
 *
 * @author campus-mart
 */
@Data
@Schema(description = "消息信息")
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "接收人ID")
    private Long userId;

    @Schema(description = "接收人昵称（后台列表用）")
    private String receiverNickname;

    @Schema(description = "消息类型：1系统 2订单 3举报 4AI")
    private Integer msgType;

    @Schema(description = "消息类型文案")
    private String msgTypeText;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "关联业务ID（订单/商品等）")
    private Long bizId;

    @Schema(description = "是否已读：0未读 1已读")
    private String isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
