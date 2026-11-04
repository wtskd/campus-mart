package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项
 *
 * @author campus-mart
 */
@Data
@Schema(description = "商品列表项")
public class ItemListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品编号")
    private String itemNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "成色 1-10（10 全新）")
    private Integer itemCondition;

    @Schema(description = "交易类型 1出售 2求购 3交换")
    private Integer tradeType;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "卖家昵称")
    private String sellerNickname;

    @Schema(description = "交易地点")
    private String location;

    @Schema(description = "状态 0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规")
    private Integer status;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "收藏量")
    private Integer favoriteCount;

    @Schema(description = "上架时间")
    private LocalDateTime publishTime;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    /** 审核意见（驳回原因，我的发布页展示） */
    private String auditRemark;

    /** 当前登录用户是否已收藏（未登录为 false） */
    private Boolean favorited;
}
