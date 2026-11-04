package com.campus.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品概要（服务间传输用）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "商品概要")
public class ItemBriefDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;

    private String itemNo;

    private String title;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private String categoryName;

    private Long sellerId;

    private String sellerName;

    private Integer status;

    private Integer itemCondition;

    private String coverUrl;

    /** JSON 数组字符串，如 ["url1","url2"] */
    private String images;

    /** JSON 数组字符串 */
    private String tags;

    private Integer viewCount;

    private Integer favoriteCount;
}
