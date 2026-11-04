package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 热门商品
 *
 * @author campus-mart
 */
@Data
@Schema(description = "热门商品")
public class HotItemDTO {

    @Schema(description = "商品 ID")
    private Long itemId;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "一级分类名称")
    private String categoryName;

    @Schema(description = "商品状态")
    private Integer status;

    @Schema(description = "商品状态描述")
    private String statusText;

    @Schema(description = "浏览量")
    private Long viewCount;

    @Schema(description = "收藏量")
    private Long favoriteCount;

    @Schema(description = "成交单数")
    private Long orderCount;

    @Schema(description = "成交金额")
    private BigDecimal gmv;
}
