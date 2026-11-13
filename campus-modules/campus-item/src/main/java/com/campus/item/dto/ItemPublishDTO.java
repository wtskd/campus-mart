package com.campus.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布商品请求
 *
 * @author campus-mart
 */
@Data
@Schema(description = "发布商品请求")
public class ItemPublishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标题，5-50 字")
    @NotBlank(message = "商品标题不能为空")
    @Size(min = 5, max = 50, message = "商品标题长度需在 5-50 个字符之间")
    private String title;

    @Schema(description = "摘要，最长 255 字")
    @Size(max = 255, message = "商品摘要不能超过 255 个字符")
    private String summary;

    @Schema(description = "详细描述，10-1000 字")
    @NotBlank(message = "商品描述不能为空")
    @Size(min = 10, max = 1000, message = "商品描述长度需在 10-1000 个字符之间")
    private String description;

    @Schema(description = "售价，0.01-99999.99")
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    @DecimalMax(value = "99999.99", message = "商品价格不能超过 99999.99")
    private BigDecimal price;

    @Schema(description = "原价")
    @DecimalMin(value = "0.01", message = "原价必须大于 0")
    @DecimalMax(value = "999999.99", message = "原价不能超过 999999.99")
    private BigDecimal originalPrice;

    @Schema(description = "分类ID")
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @Schema(description = "成色 1-10（10 全新）")
    @NotNull(message = "商品成色不能为空")
    @Min(value = 1, message = "成色取值范围 1-10")
    @Max(value = 10, message = "成色取值范围 1-10")
    private Integer itemCondition;

    @Schema(description = "交易类型 1出售 2求购 3交换")
    @Min(value = 1, message = "交易类型只能为 1、2、3")
    @Max(value = 3, message = "交易类型只能为 1、2、3")
    private Integer tradeType = 1;

    @Schema(description = "封面图")
    @Size(max = 500, message = "封面图地址不能超过 500 个字符")
    private String coverUrl;

    @Schema(description = "图片列表")
    @Size(max = 9, message = "最多上传 9 张图片")
    @NotEmpty(message = "请至少上传 1 张商品图片")
    @Size(max = 9, message = "商品图片最多 9 张")
    private List<String> images;

    @Schema(description = "标签")
    @Size(max = 10, message = "最多设置 10 个标签")
    private List<String> tags;

    @Schema(description = "交易地点")
    @Size(max = 64, message = "交易地点不能超过 64 个字符")
    private String location;

    @Schema(description = "联系方式 0站内 1手机 2微信")
    @Min(value = 0, message = "联系方式只能为 0、1、2")
    @Max(value = 2, message = "联系方式只能为 0、1、2")
    private Integer contactType = 0;
}
