package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 留存分析 DTO
 *
 * @author campus-mart
 */
@Data
@Schema(description = "留存分析")
public class RetentionDTO {

    @Schema(description = "统计天数")
    private Integer days;

    @Schema(description = "区间活跃用户数合计")
    private Long totalActiveUsers;

    @Schema(description = "平均次日留存率")
    private Double avgRate1;

    @Schema(description = "平均 3 日留存率")
    private Double avgRate3;

    @Schema(description = "平均 7 日留存率")
    private Double avgRate7;

    @Schema(description = "按活跃日期的留存明细")
    private List<RetentionPointDTO> points = new ArrayList<>();
}
