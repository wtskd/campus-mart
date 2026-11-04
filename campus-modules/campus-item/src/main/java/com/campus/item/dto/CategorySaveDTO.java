package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增分类请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "新增分类请求")
public class CategorySaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "父分类ID，0 为一级分类")
    @NotNull(message = "父分类ID不能为空")
    private Long parentId = 0L;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称不能超过 32 个字符")
    private String categoryName;

    @Schema(description = "图标")
    @Size(max = 255, message = "图标地址不能超过 255 个字符")
    private String icon;

    @Schema(description = "排序号，升序")
    private Integer orderNum = 0;

    @Schema(description = "状态 0正常 1停用")
    @Pattern(regexp = "^[01]$", message = "状态只能为 0（正常）或 1（停用）")
    private String status = "0";
}
