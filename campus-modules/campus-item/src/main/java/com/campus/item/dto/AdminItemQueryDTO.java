package com.campus.item.dto;

import com.campus.common.core.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台商品列表查询条件
 *
 * @author campus-mart
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台商品列表查询条件")
public class AdminItemQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字：标题/描述/商品编号模糊匹配")
    @Size(max = 50, message = "关键字不能超过 50 个字符")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：0草稿 1待审核 2在售 3已预订 4已售出 5下架 6违规")
    @Min(value = 0, message = "商品状态只能为 0-6")
    @Max(value = 6, message = "商品状态只能为 0-6")
    private Integer status;

    @Schema(description = "卖家ID")
    private Long sellerId;
}
