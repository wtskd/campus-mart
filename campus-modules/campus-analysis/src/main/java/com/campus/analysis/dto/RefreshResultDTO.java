package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统计重算结果
 *
 * @author campus-mart
 */
@Data
@Schema(description = "统计重算结果")
public class RefreshResultDTO {

    @Schema(description = "开始日期 yyyy-MM-dd")
    private String startDate;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private String endDate;

    @Schema(description = "stat_overview_daily 写入行数")
    private int overviewRows;

    @Schema(description = "stat_category_daily 写入行数")
    private int categoryRows;

    @Schema(description = "stat_funnel_daily 写入行数")
    private int funnelRows;

    @Schema(description = "stat_user_rfm 写入行数")
    private int rfmRows;

    @Schema(description = "写入总行数")
    private int totalRows;

    @Schema(description = "耗时（毫秒）")
    private long costMs;

    @Schema(description = "结果说明")
    private String message;
}
