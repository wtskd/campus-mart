package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发表评价请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "发表评价请求")
public class CommentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long itemId;

    @Schema(description = "订单ID（先成交后评价，必填）")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "评价内容")
    @NotBlank(message = "评价内容不能为空")
    @Size(min = 2, max = 500, message = "评价内容长度需在 2-500 个字符之间")
    private String content;

    @Schema(description = "评分 1-5")
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围 1-5")
    @Max(value = 5, message = "评分范围 1-5")
    private Integer rating;

    @Schema(description = "评价图片")
    @Size(max = 9, message = "最多上传 9 张图片")
    private List<String> images;
}
