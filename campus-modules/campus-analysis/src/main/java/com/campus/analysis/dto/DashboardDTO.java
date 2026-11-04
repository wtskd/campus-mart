package com.campus.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 数据大屏聚合 DTO（前端只调 /analysis/dashboard 一个接口）
 *
 * @author campus-mart
 */
@Data
@Schema(description = "数据大屏")
public class DashboardDTO {

    @Schema(description = "总览卡片")
    private OverviewDTO overview;

    @Schema(description = "近 7 日趋势（GMV / 订单曲线）")
    private TrendDTO trend;

    @Schema(description = "近 30 日趋势（GMV / 订单曲线，供大屏 7/30 天切换）")
    private TrendDTO trend30;

    @Schema(description = "近 30 日分类成交占比")
    private List<CategoryStatDTO> category = new ArrayList<>();

    @Schema(description = "近 7 日转化漏斗")
    private FunnelDTO funnel;

    @Schema(description = "热门商品 Top10")
    private List<HotItemDTO> hotItem = new ArrayList<>();

    @Schema(description = "近 7 日 AI 使用情况")
    private AiUsageDTO aiUsage;

    @Schema(description = "RFM 人群聚合（不含用户明细分页）")
    private List<RfmSegmentDTO> rfm = new ArrayList<>();

    @Schema(description = "最新 5 条成交动态")
    private List<RecentOrderDTO> recentOrder = new ArrayList<>();

    @Schema(description = "生成时间 yyyy-MM-dd HH:mm:ss")
    private String generatedAt;
}
