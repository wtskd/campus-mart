package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类状态修改请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "分类状态修改请求")
public class CategoryStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "状态 0正常 1停用")
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^[01]$", message = "状态只能为 0（正常）或 1（停用）")
    private String status;
}
