package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的收藏列表项
 *
 * @author campus-mart
 */
@Data
@Schema(description = "我的收藏列表项")
public class FavoriteItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏ID")
    private Long favoriteId;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "成色 1-10（10 全新）")
    private Integer itemCondition;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "商品状态 0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规")
    private Integer status;

    @Schema(description = "上架时间")
    private LocalDateTime publishTime;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;
}
