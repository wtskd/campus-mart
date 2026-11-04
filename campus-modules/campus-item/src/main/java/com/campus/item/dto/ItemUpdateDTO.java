package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编辑商品请求
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编辑商品请求")
public class ItemUpdateDTO extends ItemPublishDTO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long itemId;
}
