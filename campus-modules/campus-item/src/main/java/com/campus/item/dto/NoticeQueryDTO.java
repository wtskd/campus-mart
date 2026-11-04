package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 后台公告分页查询参数
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台公告分页查询参数")
public class NoticeQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题（模糊匹配）")
    private String title;

    @Schema(description = "公告类型：1公告 2活动 3规则")
    private Integer noticeType;

    @Schema(description = "状态：0发布 1草稿")
    private String status;

    @Schema(description = "发布时间起，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "发布时间止，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
