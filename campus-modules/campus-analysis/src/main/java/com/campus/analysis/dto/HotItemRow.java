package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 热门商品行
 *
 * @author campus-mart
 */
@Data
public class HotItemRow {

    /** 商品 ID */
    private Long itemId;

    /** 商品标题 */
    private String title;

    /** 售价 */
    private BigDecimal price;

    /** 一级分类名称 */
    private String categoryName;

    /** 商品状态 */
    private Integer status;

    /** 浏览量 */
    private Long viewCount;

    /** 收藏量 */
    private Long favoriteCount;

    /** 成交单数 */
    private Long orderCount;

    /** 成交金额 */
    private BigDecimal gmv;
}
