package com.campus.ai.domain.dto;

import com.campus.ai.support.FlexibleKeywordsDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一键发布文案请求
 *
 * <p>前端（campus-app/publish.vue）实际传的是 {@code {keywords:字符串, category, condition, price,
 * originalPrice, remark}}，本类同时兼容文档里的 {@code categoryName / originalTitle} 写法。</p>
 *
 * @author campus-mart
 */
@Data
public class CopywritingRequest {

    /** 分类名称，如"数码电子"（缺省时按"闲置好物"生成） */
    @Size(max = 50, message = "商品分类过长")
    private String categoryName;

    /** 分类名称（前端字段别名） */
    @Size(max = 50, message = "商品分类过长")
    private String category;

    /** 关键词，如 ["iPad", "平板", "学习"]；也兼容空格/逗号分隔的字符串 */
    @Size(max = 10, message = "关键词最多 10 个")
    @JsonDeserialize(using = FlexibleKeywordsDeserializer.class)
    private List<String> keywords;

    /** 成色，如"95新"、"九成新"、"全新" */
    @Size(max = 20, message = "成色描述过长")
    private String condition;

    /** 期望售价 */
    private BigDecimal price;

    /** 原价（可选） */
    private BigDecimal originalPrice;

    /** 商品标题（后台「AI 配置 · 在线试用」与文档字段） */
    @Size(max = 200, message = "商品标题过长")
    private String title;

    /** 补充说明 / 原标题 */
    @Size(max = 200, message = "补充说明过长")
    private String originalTitle;

    /** 补充说明（前端字段别名 remark） */
    @Size(max = 200, message = "补充说明过长")
    private String remark;
}
