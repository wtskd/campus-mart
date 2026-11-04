package com.campus.ai.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐商品
 *
 * @author campus-mart
 */
@Data
public class RecommendVO {

    private Long itemId;

    private String title;

    private String coverUrl;

    private BigDecimal price;

    private String categoryName;

    /** 推荐得分（0-100） */
    private Double score;

    /** 中文推荐理由（落库时截断到 64 字符） */
    private String reason;
}
