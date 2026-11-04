package com.campus.ai.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 智能估价请求
 *
 * <p>前端字段名可能是 {@code category} / {@code remark}，这里同时兼容
 * {@code categoryName} / {@code description} 两种写法。</p>
 *
 * @author campus-mart
 */
@Data
public class PriceRequest {

    /** 商品标题（可选） */
    @Size(max = 200, message = "商品标题过长")
    private String title;

    /** 分类名称（兼容字段） */
    @Size(max = 50, message = "商品分类过长")
    private String category;

    /** 分类名称 */
    @Size(max = 50, message = "商品分类过长")
    private String categoryName;

    /** 成色，如"9成新"、数字 9 */
    @Size(max = 20, message = "成色描述过长")
    private String condition;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 期望售价 */
    private BigDecimal price;

    /** 商品描述 / 补充说明 */
    @Size(max = 1000, message = "描述过长")
    private String remark;

    /** 商品描述（兼容字段） */
    @Size(max = 1000, message = "描述过长")
    private String description;

    /** 已发布商品 ID（可选，传了则自动回填商品信息） */
    private Long itemId;
}
