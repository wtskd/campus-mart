package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 漏斗层级
 *
 * @author campus-mart
 */
@Data
@Schema(description = "漏斗层级")
public class FunnelStageDTO {

    @Schema(description = "层级标识")
    private String code;

    @Schema(description = "层级名称")
    private String name;

    @Schema(description = "数量")
    private Long count;

    @Schema(description = "相对上一层的转化率（0.1234 = 12.34%）")
    private Double rate;
}
