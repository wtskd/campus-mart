package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 分类成交占比 DTO
 *
 * @author campus-mart
 */
@Data
@Schema(description = "分类成交占比")
public class CategoryStatDTO {

    @Schema(description = "一级分类 ID")
    private Long categoryId;

    @Schema(description = "一级分类名称")
    private String categoryName;

    @Schema(description = "成交金额")
    private BigDecimal gmv;

    @Schema(description = "成交件数")
    private Long orderCount;

    @Schema(description = "成交金额占比（0.1234 = 12.34%）")
    private Double gmvRatio;

    @Schema(description = "成交件数占比（0.1234 = 12.34%）")
    private Double orderRatio;
}
