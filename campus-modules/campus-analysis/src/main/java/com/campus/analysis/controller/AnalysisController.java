package com.campus.analysis.controller;

import com.campus.analysis.dto.AiUsageDTO;
import com.campus.analysis.dto.CategoryStatDTO;
import com.campus.analysis.dto.DashboardDTO;
import com.campus.analysis.dto.FunnelDTO;
import com.campus.analysis.dto.HotItemDTO;
import com.campus.analysis.dto.OverviewDTO;
import com.campus.analysis.dto.RefreshResultDTO;
import com.campus.analysis.dto.RetentionDTO;
import com.campus.analysis.dto.RfmDTO;
import com.campus.analysis.dto.SellerRankDTO;
import com.campus.analysis.dto.TrendDTO;
import com.campus.analysis.service.AnalysisService;
import com.campus.analysis.service.StatRefreshService;
import com.campus.analysis.util.AnalysisUtils;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析接口（网关路径 /api/analysis/** → StripPrefix 后 /analysis/**）。
 *
 * <p>权限码：analysis:overview / analysis:trade / analysis:user / analysis:funnel / analysis:screen /
 * analysis:ai / analysis:item / analysis:export / analysis:refresh。
 * 为兼容不同角色（管理员和运营都能看大屏），同一接口按「任一权限命中」校验（requireAll = false）。</p>
 *
 * @author campus-mart
 */
@Tag(name = "数据分析", description = "总览 / 趋势 / 分类 / 漏斗 / 留存 / RFM / 榜单 / AI 使用 / 大屏 / 导出")
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    private final StatRefreshService statRefreshService;

    @Operation(summary = "总览卡片", description = "今日 / 昨日 / 近 7 日 / 累计 GMV、订单、用户、商品、AI 调用等；Redis 缓存 60 秒")
    @RequiresPerm({"analysis:view", "analysis:overview", "analysis:trade", "analysis:user",
            "analysis:funnel", "analysis:screen"})
    @GetMapping("/overview")
    public Result<OverviewDTO> overview() {
        return Result.ok(analysisService.overview());
    }

    @Operation(summary = "趋势折线", description = "每天 GMV / 订单数 / 成交订单数 / 新增用户 / 新增商品 / AI 调用，缺数据日期补 0")
    @RequiresPerm({"analysis:view", "analysis:overview", "analysis:trade"})
    @GetMapping("/trend")
    public Result<TrendDTO> trend(
            @Parameter(description = "天数，支持 7 / 14 / 30，默认 7") @RequestParam(defaultValue = "7") int days) {
        return Result.ok(analysisService.trend(days));
    }

    @Operation(summary = "分类成交占比", description = "各一级分类成交金额占比与成交件数（饼图）")
    @RequiresPerm({"analysis:view", "analysis:trade", "analysis:overview"})
    @GetMapping("/category")
    public Result<List<CategoryStatDTO>> category(
            @Parameter(description = "天数，默认 30") @RequestParam(defaultValue = "30") int days) {
        return Result.ok(analysisService.category(days));
    }

    @Operation(summary = "转化漏斗", description = "商品曝光 → 收藏 → 下单 → 成交，每层数量与转化率")
    @RequiresPerm({"analysis:view", "analysis:funnel", "analysis:overview"})
    @GetMapping("/funnel")
    public Result<FunnelDTO> funnel(
            @Parameter(description = "天数，默认 7") @RequestParam(defaultValue = "7") int days) {
        return Result.ok(analysisService.funnel(days));
    }

    @Operation(summary = "用户留存", description = "以当日有下单或收藏行为的用户为活跃，计算次日 / 3 日 / 7 日留存（近 30 天）")
    @RequiresPerm({"analysis:view", "analysis:user", "analysis:funnel"})
    @GetMapping("/retention")
    public Result<RetentionDTO> retention(
            @Parameter(description = "天数，默认 7（最多 30）") @RequestParam(defaultValue = "7") int days) {
        return Result.ok(analysisService.retention(days));
    }

    @Operation(summary = "RFM 用户分层", description = "8 类人群人数与金额占比 + 明细分页；无成交用户标记为流失")
    @RequiresPerm({"analysis:view", "analysis:user"})
    @GetMapping("/rfm")
    public Result<RfmDTO> rfm(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 10，最大 100") @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(analysisService.rfm(page, limit));
    }

    @Operation(summary = "热门商品榜", description = "按浏览量 + 收藏量加权排序，附带成交量 / 成交额")
    @RequiresPerm({"analysis:view", "analysis:item", "analysis:trade"})
    @GetMapping({"/hot-item", "/hot-items"})
    public Result<List<HotItemDTO>> hotItem(
            @Parameter(description = "条数，默认 10") @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(analysisService.hotItem(limit));
    }

    @Operation(summary = "卖家榜", description = "按成交额排序，附带成交单数与好评率")
    @RequiresPerm({"analysis:view", "analysis:trade"})
    @GetMapping("/seller-rank")
    public Result<List<SellerRankDTO>> sellerRank(
            @Parameter(description = "条数，默认 10") @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(analysisService.sellerRank(limit));
    }

    @Operation(summary = "AI 使用情况", description = "按场景统计调用次数 / 成功率 / 平均耗时 / token 消耗；无数据返回空数组")
    @RequiresPerm({"analysis:view", "analysis:ai"})
    @GetMapping("/ai-usage")
    public Result<AiUsageDTO> aiUsage(
            @Parameter(description = "天数，默认 7") @RequestParam(defaultValue = "7") int days) {
        return Result.ok(analysisService.aiUsage(days));
    }

    @Operation(summary = "数据大屏", description = "一次返回总览 + 趋势(7/30) + 分类 + 漏斗 + 热门商品 + AI 使用 + RFM 聚合 + 最新成交动态；Redis 缓存 30 秒；兼容 /screen 路径")
    @RequiresPerm({"analysis:view", "analysis:screen", "analysis:overview"})
    @GetMapping({"/dashboard", "/screen"})
    public Result<DashboardDTO> dashboard() {
        return Result.ok(analysisService.dashboard());
    }

    @Operation(summary = "手动重算统计", description = "重算指定区间（默认最近 7 天）的 4 张 stat_* 预聚合表，返回每张表写入行数")
    @RequiresPerm({"analysis:view", "analysis:refresh"})
    @OperLog(title = "统计重算", businessType = 2)
    @PostMapping({"/refresh", "/rebuild"})
    public Result<RefreshResultDTO> refresh(
            @Parameter(description = "开始日期 yyyy-MM-dd，默认 7 天前")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd，默认今天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(statRefreshService.refreshRange(startDate, endDate));
    }

    @Operation(summary = "导出订单 CSV", description = "UTF-8 BOM 防止 Excel 乱码，Content-Disposition: attachment")
    @RequiresPerm({"analysis:view", "analysis:export"})
    @OperLog(title = "订单导出", businessType = 5, saveResponseData = false)
    @GetMapping("/export/order")
    public ResponseEntity<byte[]> exportOrder(
            @Parameter(description = "开始日期 yyyy-MM-dd，默认 7 天前")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd，默认今天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate == null ? AnalysisUtils.today() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        String csv = analysisService.exportOrderCsv(startDate, endDate);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"order_" + start + "_" + end + ".csv\"");
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
