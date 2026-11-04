package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播新增/修改请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "轮播新增/修改请求")
public class BannerSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "轮播ID，修改时必填")
    private Long bannerId;

    @Schema(description = "轮播标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "轮播标题不能为空")
    @Size(max = 64, message = "轮播标题不能超过 64 个字符")
    private String title;

    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图片地址不能为空")
    @Size(max = 500, message = "图片地址不能超过 500 个字符")
    private String imageUrl;

    @Schema(description = "跳转链接")
    @Size(max = 500, message = "跳转链接不能超过 500 个字符")
    private String linkUrl;

    @Schema(description = "排序号，升序")
    private Integer sort = 0;

    @Schema(description = "状态：0上架 1下架")
    @Pattern(regexp = "^[01]$", message = "状态只能为 0（上架）或 1（下架）")
    private String status = "0";

    @Schema(description = "生效开始时间，为空表示立即生效")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间，为空表示长期有效")
    private LocalDateTime endTime;
}
