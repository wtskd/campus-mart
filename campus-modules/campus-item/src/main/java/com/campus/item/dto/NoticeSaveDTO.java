package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告新增/修改请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "公告新增/修改请求")
public class NoticeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID，修改时必填")
    private Long noticeId;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过 100 个字符")
    private String title;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "公告类型：1公告 2活动 3规则")
    @Min(value = 1, message = "公告类型不合法")
    @Max(value = 3, message = "公告类型不合法")
    private Integer noticeType = 1;

    @Schema(description = "状态：0发布 1草稿")
    @Pattern(regexp = "^[01]$", message = "状态只能为 0（发布）或 1（草稿）")
    private String status = "0";

    @Schema(description = "发布时间，发布状态且为空时默认当前时间")
    private LocalDateTime publishTime;
}
