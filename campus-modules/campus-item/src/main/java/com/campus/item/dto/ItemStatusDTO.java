package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品状态修改请求（后台强制下架/违规）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "商品状态修改请求")
public class ItemStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long itemId;

    @Schema(description = "目标状态：0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规")
    @NotNull(message = "目标状态不能为空")
    @Min(value = 0, message = "商品状态只能为 0-6")
    @Max(value = 6, message = "商品状态只能为 0-6")
    private Integer status;

    @Schema(description = "备注（违规原因等）")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
