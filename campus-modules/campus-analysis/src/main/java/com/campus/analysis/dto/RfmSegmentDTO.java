package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * RFM 人群汇总
 *
 * @author campus-mart
 */
@Data
@Schema(description = "RFM 人群汇总")
public class RfmSegmentDTO {

    @Schema(description = "人群名称")
    private String segment;

    @Schema(description = "用户数")
    private Long userCount;

    @Schema(description = "用户数占比（0.1234 = 12.34%）")
    private Double userRatio;

    @Schema(description = "成交金额")
    private BigDecimal monetary;

    @Schema(description = "人均成交金额")
    private BigDecimal avgMonetary;

    @Schema(description = "成交金额占比（0.1234 = 12.34%）")
    private Double monetaryRatio;
}
