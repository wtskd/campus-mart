package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台举报分页查询参数
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台举报分页查询参数")
public class ReportQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "举报人ID")
    private Long reporterId;

    @Schema(description = "状态：0待处理 1已处理 2驳回")
    private Integer status;

    @Schema(description = "举报时间起，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "举报时间止，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
