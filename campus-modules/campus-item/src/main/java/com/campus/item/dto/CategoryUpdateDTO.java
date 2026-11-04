package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编辑分类请求
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编辑分类请求")
public class CategoryUpdateDTO extends CategorySaveDTO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
