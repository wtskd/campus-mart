package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 前台商品列表查询条件
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "前台商品列表查询条件")
public class AppItemQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字：标题/描述模糊匹配")
    @Size(max = 50, message = "关键字不能超过 50 个字符")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "最低价格")
    @DecimalMin(value = "0", message = "最低价格不能小于 0")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    @DecimalMin(value = "0", message = "最高价格不能小于 0")
    private BigDecimal maxPrice;

    @Schema(description = "成色 1-10（10 全新）")
    @Min(value = 1, message = "成色取值范围 1-10")
    @Max(value = 10, message = "成色取值范围 1-10")
    private Integer itemCondition;

    @Schema(description = "排序：new 最新（默认）/ priceAsc 价格升序 / priceDesc 价格降序 / hot 最热")
    @Pattern(regexp = "^(new|priceAsc|priceDesc|hot)$", message = "排序方式只能为 new、priceAsc、priceDesc、hot")
    private String sort;
}
