package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报视图对象
 *
 * @author campus-mart
 */
@Data
@Schema(description = "举报信息")
public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "举报ID")
    private Long reportId;

    @Schema(description = "举报对象类型，当前仅 item")
    private String targetType = "item";

    @Schema(description = "举报对象ID（商品ID）")
    private Long itemId;

    @Schema(description = "举报对象标题（商品标题）")
    private String itemTitle;

    @Schema(description = "举报对象封面（商品封面）")
    private String itemCover;

    @Schema(description = "举报人ID")
    private Long reporterId;

    @Schema(description = "举报人昵称")
    private String reporterNickname;

    @Schema(description = "举报原因")
    private String reasonType;

    @Schema(description = "补充说明")
    private String content;

    @Schema(description = "状态：0待处理 1已处理 2驳回")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "处理人ID")
    private Long handleUserId;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "举报时间")
    private LocalDateTime createTime;
}
