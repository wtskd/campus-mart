package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 消息分页查询参数
 *
 * <p>前台忽略 userId（固定查询当前登录用户），后台可按接收人筛选。</p>
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息分页查询参数")
public class MessageQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "接收人ID（仅后台使用）")
    private Long userId;

    @Schema(description = "消息类型：1系统 2订单 3举报 4AI")
    private Integer msgType;

    @Schema(description = "是否已读：0未读 1已读")
    private String isRead;

    @Schema(description = "创建时间起，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "创建时间止，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
