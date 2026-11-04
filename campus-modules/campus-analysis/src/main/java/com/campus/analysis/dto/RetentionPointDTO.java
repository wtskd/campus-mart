package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 留存数据点（按活跃日期分组）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "留存数据点")
public class RetentionPointDTO {

    @Schema(description = "活跃日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "当日活跃用户数")
    private Long activeUsers;

    @Schema(description = "次日留存用户数")
    private Long retained1;

    @Schema(description = "3 日留存用户数")
    private Long retained3;

    @Schema(description = "7 日留存用户数")
    private Long retained7;

    @Schema(description = "次日留存率（0.1234 = 12.34%）")
    private Double rate1;

    @Schema(description = "3 日留存率（0.1234 = 12.34%）")
    private Double rate3;

    @Schema(description = "7 日留存率（0.1234 = 12.34%）")
    private Double rate7;
}
