package com.campus.analysis.service.impl;

import com.campus.analysis.constant.AnalysisConstants;
import com.campus.analysis.constant.ItemStatus;
import com.campus.analysis.constant.OrderStatus;
import com.campus.analysis.dto.AiSceneRow;
import com.campus.analysis.dto.AiSceneStatDTO;
import com.campus.analysis.dto.AiUsageDTO;
import com.campus.analysis.dto.CategoryAggRow;
import com.campus.analysis.dto.CategoryStatDTO;
import com.campus.analysis.dto.DailyStatRow;
import com.campus.analysis.dto.DashboardDTO;
import com.campus.analysis.dto.FunnelDTO;
import com.campus.analysis.dto.FunnelRow;
import com.campus.analysis.dto.FunnelStageDTO;
import com.campus.analysis.dto.HotItemDTO;
import com.campus.analysis.dto.HotItemRow;
import com.campus.analysis.dto.OrderExportRow;
import com.campus.analysis.dto.OverviewDTO;
import com.campus.analysis.dto.OverviewTotalsRow;
import com.campus.analysis.dto.RecentOrderDTO;
import com.campus.analysis.dto.RecentOrderRow;
import com.campus.analysis.dto.RetentionDTO;
import com.campus.analysis.dto.RetentionPointDTO;
import com.campus.analysis.dto.RetentionRow;
import com.campus.analysis.dto.RfmDTO;
import com.campus.analysis.dto.SellerRankDTO;
import com.campus.analysis.dto.SellerRankRow;
import com.campus.analysis.dto.TrendDTO;
import com.campus.analysis.dto.TrendPointDTO;
import com.campus.analysis.mapper.StatCustomMapper;
import com.campus.analysis.service.AnalysisService;
import com.campus.analysis.service.RfmService;
import com.campus.analysis.service.StatRefreshService;
import com.campus.analysis.util.AnalysisUtils;
import com.campus.common.utils.RedisUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据分析查询服务实现。
 *
 * <p>统计口径：</p>
 * <ul>
 *   <li>成交订单 = biz_order.status = 2（{@link OrderStatus#FINISHED}）；</li>
 *   <li>成交额按完成时间归属（缺失回退下单时间）；下单数按下单时间归属；</li>
 *   <li>商品曝光 = biz_item.view_count 汇总；收藏 = biz_favorite 行数；</li>
 *   <li>学生用户 = sys_user.user_type = '01'；活跃 = 当日有下单或收藏行为的用户去重；</li>
 *   <li>金额 BigDecimal 保留 2 位小数，比例 Double 保留 4 位小数（0.1234 = 12.34%）。</li>
 * </ul>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    /** 趋势 / 留存等查询允许的最大天数 */
    private static final int MAX_DAYS = 90;

    /** 留存只统计近 30 天 */
    private static final int MAX_RETENTION_DAYS = 30;

    /** 单页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 榜单最大条数 */
    private static final int MAX_RANK_LIMIT = 50;

    private final StatCustomMapper statMapper;

    private final StatRefreshService statRefreshService;

    private final RfmService rfmService;

    private final RedisUtils redisUtils;

    // ==================================================================================
    // 总览
    // ==================================================================================

    @Override
    public OverviewDTO overview() {
        OverviewDTO cached = cacheGet(AnalysisConstants.CACHE_OVERVIEW, OverviewDTO.class);
        if (cached != null) {
            return cached;
        }
        OverviewDTO dto = buildOverview();
        cacheSet(AnalysisConstants.CACHE_OVERVIEW, dto, AnalysisConstants.CACHE_OVERVIEW_SECONDS);
        return dto;
    }

    /**
     * 构建总览（不走缓存，供 /overview 与 /dashboard 复用）。
     *
     * @return 总览
     */
    private OverviewDTO buildOverview() {
        LocalDate today = AnalysisUtils.today();
        LocalDate start = today.minusDays(AnalysisConstants.DEFAULT_TREND_DAYS - 1L);
        List<DailyStatRow> rows = statRefreshService.overviewRows(start, today);
        DailyStatRow todayRow = rows.isEmpty() ? zeroDay(today) : rows.get(rows.size() - 1);
        DailyStatRow yesterdayRow = rows.size() >= 2 ? rows.get(rows.size() - 2) : zeroDay(today.minusDays(1));

        BigDecimal weekGmv = BigDecimal.ZERO;
        long weekOrder = 0L;
        long weekFinish = 0L;
        long weekNewUser = 0L;
        long weekNewItem = 0L;
        long weekAi = 0L;
        for (DailyStatRow row : rows) {
            weekGmv = weekGmv.add(AnalysisUtils.money(row.getGmv()));
            weekOrder += AnalysisUtils.nvl(row.getOrderCount());
            weekFinish += AnalysisUtils.nvl(row.getFinishOrderCount());
            weekNewUser += AnalysisUtils.nvl(row.getNewUserCount());
            weekNewItem += AnalysisUtils.nvl(row.getItemPublishCount());
            weekAi += AnalysisUtils.nvl(row.getAiCallCount());
        }
        weekGmv = AnalysisUtils.money(weekGmv);

        OverviewTotalsRow totals = statMapper.selectOverviewTotals();
        if (totals == null) {
            totals = new OverviewTotalsRow();
        }

        OverviewDTO dto = new OverviewDTO();
        dto.setStatDate(AnalysisUtils.format(today));

        dto.setTodayGmv(AnalysisUtils.money(todayRow.getGmv()));
        dto.setTodayOrderCount(AnalysisUtils.nvl(todayRow.getOrderCount()));
        dto.setTodayFinishOrderCount(AnalysisUtils.nvl(todayRow.getFinishOrderCount()));
        dto.setTodayNewUserCount(AnalysisUtils.nvl(todayRow.getNewUserCount()));
        dto.setTodayNewItemCount(AnalysisUtils.nvl(todayRow.getItemPublishCount()));
        dto.setTodayAiCallCount(AnalysisUtils.nvl(todayRow.getAiCallCount()));
        dto.setTodayAvgOrderAmount(AnalysisUtils.avgOrderAmount(todayRow.getGmv(),
                AnalysisUtils.nvl(todayRow.getFinishOrderCount())));

        dto.setYesterdayGmv(AnalysisUtils.money(yesterdayRow.getGmv()));
        dto.setYesterdayOrderCount(AnalysisUtils.nvl(yesterdayRow.getOrderCount()));
        dto.setYesterdayFinishOrderCount(AnalysisUtils.nvl(yesterdayRow.getFinishOrderCount()));
        dto.setYesterdayNewUserCount(AnalysisUtils.nvl(yesterdayRow.getNewUserCount()));
        dto.setYesterdayNewItemCount(AnalysisUtils.nvl(yesterdayRow.getItemPublishCount()));
        dto.setYesterdayAiCallCount(AnalysisUtils.nvl(yesterdayRow.getAiCallCount()));
        dto.setYesterdayAvgOrderAmount(AnalysisUtils.avgOrderAmount(yesterdayRow.getGmv(),
                AnalysisUtils.nvl(yesterdayRow.getFinishOrderCount())));

        dto.setWeekGmv(weekGmv);
        dto.setWeekOrderCount(weekOrder);
        dto.setWeekFinishOrderCount(weekFinish);
        dto.setWeekNewUserCount(weekNewUser);
        dto.setWeekNewItemCount(weekNewItem);
        dto.setWeekAiCallCount(weekAi);
        dto.setWeekAvgOrderAmount(AnalysisUtils.avgOrderAmount(weekGmv, weekFinish));

        dto.setTotalGmv(AnalysisUtils.money(totals.getTotalGmv()));
        dto.setTotalOrderCount(AnalysisUtils.nvl(totals.getTotalOrderCount()));
        dto.setTotalFinishOrderCount(AnalysisUtils.nvl(totals.getTotalFinishOrderCount()));
        dto.setTotalUserCount(AnalysisUtils.nvl(totals.getTotalUserCount()));
        dto.setTotalItemCount(AnalysisUtils.nvl(totals.getTotalItemCount()));
        dto.setTotalAiCallCount(AnalysisUtils.nvl(totals.getTotalAiCallCount()));
        dto.setTotalFavoriteCount(AnalysisUtils.nvl(totals.getTotalFavoriteCount()));
        dto.setTotalViewCount(AnalysisUtils.nvl(totals.getTotalViewCount()));
        dto.setOnSaleItemCount(AnalysisUtils.nvl(totals.getOnSaleItemCount()));
        dto.setPendingAuditItemCount(AnalysisUtils.nvl(totals.getPendingAuditItemCount()));
        dto.setAvgOrderAmount(AnalysisUtils.avgOrderAmount(totals.getTotalGmv(),
                AnalysisUtils.nvl(totals.getTotalFinishOrderCount())));

        dto.setGmvCompare(AnalysisUtils.compare(todayRow.getGmv(), yesterdayRow.getGmv()));
        dto.setOrderCompare(AnalysisUtils.compare(BigDecimal.valueOf(AnalysisUtils.nvl(todayRow.getOrderCount())),
                BigDecimal.valueOf(AnalysisUtils.nvl(yesterdayRow.getOrderCount()))));
        return dto;
    }

    // ==================================================================================
    // 趋势
    // ==================================================================================

    @Override
    public TrendDTO trend(int days) {
        int d = normalizeDays(days, AnalysisConstants.DEFAULT_TREND_DAYS);
        LocalDate end = AnalysisUtils.today();
        LocalDate start = end.minusDays(d - 1L);
        List<DailyStatRow> rows = statRefreshService.overviewRows(start, end);

        TrendDTO dto = new TrendDTO();
        dto.setDays(d);
        BigDecimal totalGmv = BigDecimal.ZERO;
        long totalOrder = 0L;
        long totalFinish = 0L;
        long totalNewUser = 0L;
        long totalNewItem = 0L;
        long totalAi = 0L;
        for (DailyStatRow row : rows) {
            TrendPointDTO point = new TrendPointDTO();
            point.setDate(AnalysisUtils.format(row.getStatDate()));
            point.setGmv(AnalysisUtils.money(row.getGmv()));
            point.setOrderCount(AnalysisUtils.nvl(row.getOrderCount()));
            point.setFinishOrderCount(AnalysisUtils.nvl(row.getFinishOrderCount()));
            point.setNewUserCount(AnalysisUtils.nvl(row.getNewUserCount()));
            point.setNewItemCount(AnalysisUtils.nvl(row.getItemPublishCount()));
            point.setAiCallCount(AnalysisUtils.nvl(row.getAiCallCount()));
            dto.getDates().add(point.getDate());
            dto.getPoints().add(point);

            totalGmv = totalGmv.add(point.getGmv());
            totalOrder += point.getOrderCount();
            totalFinish += point.getFinishOrderCount();
            totalNewUser += point.getNewUserCount();
            totalNewItem += point.getNewItemCount();
            totalAi += point.getAiCallCount();
        }
        dto.setTotalGmv(AnalysisUtils.money(totalGmv));
        dto.setTotalOrderCount(totalOrder);
        dto.setTotalFinishOrderCount(totalFinish);
        dto.setTotalNewUserCount(totalNewUser);
        dto.setTotalNewItemCount(totalNewItem);
        dto.setTotalAiCallCount(totalAi);
        return dto;
    }

    // ==================================================================================
    // 分类
    // ==================================================================================

    @Override
    public List<CategoryStatDTO> category(int days) {
        int d = normalizeDays(days, AnalysisConstants.DEFAULT_DAYS);
        LocalDate end = AnalysisUtils.today();
        LocalDate start = end.minusDays(d - 1L);
        List<CategoryAggRow> rows = statRefreshService.categoryRows(start, end);

        long totalOrder = 0L;
        BigDecimal totalGmv = BigDecimal.ZERO;
        for (CategoryAggRow row : rows) {
            totalOrder += AnalysisUtils.nvl(row.getOrderCount());
            totalGmv = totalGmv.add(AnalysisUtils.money(row.getGmv()));
        }
        List<CategoryStatDTO> result = new ArrayList<>(rows.size());
        for (CategoryAggRow row : rows) {
            CategoryStatDTO dto = new CategoryStatDTO();
            dto.setCategoryId(row.getCategoryId());
            dto.setCategoryName(row.getCategoryName() == null ? "未分类" : row.getCategoryName());
            dto.setGmv(AnalysisUtils.money(row.getGmv()));
            dto.setOrderCount(AnalysisUtils.nvl(row.getOrderCount()));
            dto.setGmvRatio(AnalysisUtils.rate(dto.getGmv(), totalGmv));
            dto.setOrderRatio(AnalysisUtils.rate(dto.getOrderCount(), totalOrder));
            result.add(dto);
        }
        return result;
    }

    // ==================================================================================
    // 漏斗
    // ==================================================================================

    @Override
    public FunnelDTO funnel(int days) {
        int d = normalizeDays(days, AnalysisConstants.DEFAULT_TREND_DAYS);
        LocalDate end = AnalysisUtils.today();
        LocalDate start = end.minusDays(d - 1L);
        List<FunnelRow> rows = statRefreshService.funnelRows(start, end);

        long view = 0L;
        long favorite = 0L;
        long order = 0L;
        long finish = 0L;
        for (FunnelRow row : rows) {
            view += AnalysisUtils.nvl(row.getViewCount());
            favorite += AnalysisUtils.nvl(row.getFavoriteCount());
            order += AnalysisUtils.nvl(row.getOrderCount());
            finish += AnalysisUtils.nvl(row.getFinishCount());
        }

        FunnelDTO dto = new FunnelDTO();
        dto.setDays(d);
        dto.setViewCount(view);
        dto.setFavoriteCount(favorite);
        dto.setOrderCount(order);
        dto.setFinishCount(finish);
        dto.setViewToFavorite(AnalysisUtils.rate(favorite, view));
        dto.setFavoriteToOrder(AnalysisUtils.rate(order, favorite));
        dto.setOrderToFinish(AnalysisUtils.rate(finish, order));
        dto.setViewToFinish(AnalysisUtils.rate(finish, view));

        List<FunnelStageDTO> stages = new ArrayList<>(4);
        stages.add(stage("exposure", "商品曝光", view, view > 0 ? 1D : 0D));
        stages.add(stage("favorite", "收藏", favorite, dto.getViewToFavorite()));
        stages.add(stage("order", "下单", order, dto.getFavoriteToOrder()));
        stages.add(stage("finish", "成交", finish, dto.getOrderToFinish()));
        dto.setStages(stages);
        return dto;
    }

    private FunnelStageDTO stage(String code, String name, long count, Double rate) {
        FunnelStageDTO dto = new FunnelStageDTO();
        dto.setCode(code);
        dto.setName(name);
        dto.setCount(count);
        dto.setRate(rate);
        return dto;
    }

    // ==================================================================================
    // 留存
    // ==================================================================================

    @Override
    public RetentionDTO retention(int days) {
        int d = normalizeDays(days, AnalysisConstants.DEFAULT_TREND_DAYS);
        if (d > MAX_RETENTION_DAYS) {
            d = MAX_RETENTION_DAYS;
        }
        LocalDate end = AnalysisUtils.today();
        LocalDate start = end.minusDays(d - 1L);
        List<RetentionRow> rows = statMapper.selectRetentionRows(start, end);
        // 按日期建索引：SQL 只返回「当天有活跃用户」的日期，缺的日期在这里补 0，
        // 保证 days=7 就返回 7 个点，前端折线图不会因为某几天没人下单/收藏就断成 1 个点
        Map<String, RetentionRow> rowMap = new LinkedHashMap<>();
        for (RetentionRow row : rows) {
            if (row != null && row.getCohortDate() != null) {
                rowMap.put(AnalysisUtils.format(row.getCohortDate()), row);
            }
        }

        RetentionDTO dto = new RetentionDTO();
        dto.setDays(d);
        long totalActive = 0L;
        long total1 = 0L;
        long total3 = 0L;
        long total7 = 0L;
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            String date = AnalysisUtils.format(cursor);
            RetentionRow row = rowMap.get(date);
            long active = row == null ? 0L : AnalysisUtils.nvl(row.getActiveUsers());
            long retained1 = row == null ? 0L : AnalysisUtils.nvl(row.getRetained1());
            long retained3 = row == null ? 0L : AnalysisUtils.nvl(row.getRetained3());
            long retained7 = row == null ? 0L : AnalysisUtils.nvl(row.getRetained7());
            RetentionPointDTO point = new RetentionPointDTO();
            point.setDate(date);
            point.setActiveUsers(active);
            point.setRetained1(retained1);
            point.setRetained3(retained3);
            point.setRetained7(retained7);
            point.setRate1(AnalysisUtils.rate(retained1, active));
            point.setRate3(AnalysisUtils.rate(retained3, active));
            point.setRate7(AnalysisUtils.rate(retained7, active));
            dto.getPoints().add(point);

            totalActive += active;
            total1 += retained1;
            total3 += retained3;
            total7 += retained7;
        }
        dto.setTotalActiveUsers(totalActive);
        dto.setAvgRate1(AnalysisUtils.rate(total1, totalActive));
        dto.setAvgRate3(AnalysisUtils.rate(total3, totalActive));
        dto.setAvgRate7(AnalysisUtils.rate(total7, totalActive));
        return dto;
    }

    // ==================================================================================
    // RFM
    // ==================================================================================

    @Override
    public RfmDTO rfm(int page, int limit) {
        return rfmService.rfm(page, limit);
    }

    // ==================================================================================
    // 榜单
    // ==================================================================================

    @Override
    public List<HotItemDTO> hotItem(int limit) {
        int n = clampLimit(limit, 10);
        List<HotItemRow> rows = statMapper.selectHotItemRank(n);
        List<HotItemDTO> result = new ArrayList<>(rows.size());
        for (HotItemRow row : rows) {
            HotItemDTO dto = new HotItemDTO();
            dto.setItemId(row.getItemId());
            dto.setTitle(row.getTitle());
            dto.setPrice(AnalysisUtils.money(row.getPrice()));
            dto.setCategoryName(row.getCategoryName());
            dto.setStatus(row.getStatus());
            dto.setStatusText(ItemStatus.text(row.getStatus()));
            dto.setViewCount(AnalysisUtils.nvl(row.getViewCount()));
            dto.setFavoriteCount(AnalysisUtils.nvl(row.getFavoriteCount()));
            dto.setOrderCount(AnalysisUtils.nvl(row.getOrderCount()));
            dto.setGmv(AnalysisUtils.money(row.getGmv()));
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<SellerRankDTO> sellerRank(int limit) {
        int n = clampLimit(limit, 10);
        List<SellerRankRow> rows = statMapper.selectSellerRank(n);
        List<SellerRankDTO> result = new ArrayList<>(rows.size());
        for (SellerRankRow row : rows) {
            SellerRankDTO dto = new SellerRankDTO();
            dto.setSellerId(row.getSellerId());
            dto.setNickname(row.getNickname());
            dto.setDeptName(row.getDeptName());
            dto.setAmount(AnalysisUtils.money(row.getAmount()));
            dto.setOrderCount(AnalysisUtils.nvl(row.getOrderCount()));
            dto.setCommentCount(AnalysisUtils.nvl(row.getCommentCount()));
            dto.setGoodRate(AnalysisUtils.rate(AnalysisUtils.nvl(row.getGoodCount()),
                    AnalysisUtils.nvl(row.getCommentCount())));
            result.add(dto);
        }
        return result;
    }

    // ==================================================================================
    // AI 使用
    // ==================================================================================

    @Override
    public AiUsageDTO aiUsage(int days) {
        int d = normalizeDays(days, AnalysisConstants.DEFAULT_TREND_DAYS);
        LocalDate end = AnalysisUtils.today();
        LocalDate start = end.minusDays(d - 1L);
        List<AiSceneRow> rows = statMapper.selectAiUsage(start, end);

        AiUsageDTO dto = new AiUsageDTO();
        dto.setDays(d);
        long totalCall = 0L;
        long totalSuccess = 0L;
        long totalFallback = 0L;
        long totalFail = 0L;
        long totalTokens = 0L;
        double weightedCost = 0D;
        for (AiSceneRow row : rows) {
            long call = AnalysisUtils.nvl(row.getCallCount());
            long success = AnalysisUtils.nvl(row.getSuccessCount());
            long fallback = AnalysisUtils.nvl(row.getFallbackCount());
            long fail = AnalysisUtils.nvl(row.getFailCount());
            long tokens = AnalysisUtils.nvl(row.getTotalTokens());
            double avgCost = row.getAvgCostMs() == null ? 0D : row.getAvgCostMs();

            AiSceneStatDTO scene = new AiSceneStatDTO();
            scene.setScene(row.getScene());
            scene.setCallCount(call);
            scene.setSuccessCount(success);
            scene.setFallbackCount(fallback);
            scene.setFailCount(fail);
            scene.setSuccessRate(AnalysisUtils.rate(success, call));
            scene.setAvgCostMs(round4(avgCost));
            scene.setTotalTokens(tokens);
            scene.setAvgTokens(call > 0 ? round4((double) tokens / call) : 0D);
            dto.getScenes().add(scene);

            totalCall += call;
            totalSuccess += success;
            totalFallback += fallback;
            totalFail += fail;
            totalTokens += tokens;
            weightedCost += avgCost * call;
        }
        dto.setTotalCallCount(totalCall);
        dto.setSuccessCount(totalSuccess);
        dto.setFallbackCount(totalFallback);
        dto.setFailCount(totalFail);
        dto.setSuccessRate(AnalysisUtils.rate(totalSuccess, totalCall));
        dto.setAvgCostMs(totalCall > 0 ? round4(weightedCost / totalCall) : 0D);
        dto.setTotalTokens(totalTokens);
        return dto;
    }

    // ==================================================================================
    // 大屏
    // ==================================================================================

    @Override
    public DashboardDTO dashboard() {
        DashboardDTO cached = cacheGet(AnalysisConstants.CACHE_DASHBOARD, DashboardDTO.class);
        if (cached != null) {
            return cached;
        }
        DashboardDTO dto = new DashboardDTO();
        dto.setOverview(buildOverview());
        dto.setTrend(trend(AnalysisConstants.DEFAULT_TREND_DAYS));
        dto.setTrend30(trend(AnalysisConstants.DEFAULT_DAYS));
        dto.setCategory(category(AnalysisConstants.DEFAULT_DAYS));
        dto.setFunnel(funnel(AnalysisConstants.DEFAULT_TREND_DAYS));
        dto.setHotItem(hotItem(10));
        dto.setAiUsage(aiUsage(AnalysisConstants.DEFAULT_TREND_DAYS));
        dto.setRecentOrder(recentOrders(5));
        // 大屏只要 RFM 聚合段，用户明细走 /analysis/rfm 分页接口；失败不影响大屏其余部分
        try {
            dto.setRfm(rfmService.segments());
        } catch (Exception e) {
            log.warn("[analysis] RFM 聚合失败，大屏该模块降级为空：{}", e.getMessage());
        }
        dto.setGeneratedAt(AnalysisUtils.format(AnalysisUtils.now()));
        cacheSet(AnalysisConstants.CACHE_DASHBOARD, dto, AnalysisConstants.CACHE_DASHBOARD_SECONDS);
        return dto;
    }

    /**
     * 最新成交动态。
     *
     * @param limit 条数
     * @return 成交订单
     */
    private List<RecentOrderDTO> recentOrders(int limit) {
        List<RecentOrderRow> rows = statMapper.selectRecentFinishedOrders(clampLimit(limit, 5));
        List<RecentOrderDTO> result = new ArrayList<>(rows.size());
        for (RecentOrderRow row : rows) {
            RecentOrderDTO dto = new RecentOrderDTO();
            dto.setOrderNo(row.getOrderNo());
            dto.setItemTitle(row.getItemTitle());
            dto.setAmount(AnalysisUtils.money(row.getAmount()));
            dto.setBuyerNickname(row.getBuyerNickname());
            dto.setSellerNickname(row.getSellerNickname());
            dto.setFinishTime(row.getFinishTime());
            result.add(dto);
        }
        return result;
    }

    // ==================================================================================
    // 导出
    // ==================================================================================

    @Override
    public String exportOrderCsv(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? AnalysisUtils.today() : endDate;
        LocalDate start = startDate == null ? end.minusDays(AnalysisConstants.DEFAULT_TREND_DAYS - 1L) : startDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        List<OrderExportRow> rows = statMapper.selectOrderExportRows(start, end);

        StringBuilder sb = new StringBuilder(512 + rows.size() * 128);
        // UTF-8 BOM，避免 Excel 打开中文乱码
        sb.append('\uFEFF');
        sb.append("订单号,商品标题,买家,卖家,金额(元),订单状态,下单时间,成交时间,交易地点\n");
        for (OrderExportRow row : rows) {
            sb.append(csv(row.getOrderNo())).append(',')
                    .append(csv(row.getItemTitle())).append(',')
                    .append(csv(row.getBuyerName())).append(',')
                    .append(csv(row.getSellerName())).append(',')
                    .append(row.getAmount() == null ? "0.00" : AnalysisUtils.money(row.getAmount()).toPlainString()).append(',')
                    .append(csv(OrderStatus.text(row.getStatus()))).append(',')
                    .append(csv(AnalysisUtils.format(row.getCreateTime()))).append(',')
                    .append(csv(AnalysisUtils.format(row.getFinishTime()))).append(',')
                    .append(csv(row.getTradePlace())).append('\n');
        }
        return sb.toString();
    }

    /**
     * CSV 字段转义（含逗号 / 引号 / 换行时用双引号包裹，内部引号翻倍）。
     *
     * @param value 原值
     * @return 转义后的字段
     */
    private String csv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.indexOf(',') >= 0 || escaped.indexOf('"') >= 0
                || escaped.indexOf('\n') >= 0 || escaped.indexOf('\r') >= 0) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ==================================================================================
    // 小工具
    // ==================================================================================

    /**
     * 天数规整：非法值取默认值，上限 {@link #MAX_DAYS}。
     *
     * @param days    请求天数
     * @param defaultDays 默认天数
     * @return 规整后的天数
     */
    private int normalizeDays(int days, int defaultDays) {
        if (days <= 0) {
            return defaultDays;
        }
        return Math.min(days, MAX_DAYS);
    }

    /**
     * 榜单条数规整：非法值取默认值，上限 {@link #MAX_RANK_LIMIT}。
     *
     * @param limit       请求条数
     * @param defaultLimit 默认条数
     * @return 规整后的条数
     */
    private int clampLimit(int limit, int defaultLimit) {
        if (limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, MAX_RANK_LIMIT);
    }

    /**
     * 空日期行（全 0）。
     *
     * @param date 日期
     * @return 0 值行
     */
    private DailyStatRow zeroDay(LocalDate date) {
        DailyStatRow row = new DailyStatRow();
        row.setStatDate(date);
        row.setGmv(BigDecimal.ZERO);
        row.setOrderCount(0L);
        row.setFinishOrderCount(0L);
        row.setCancelOrderCount(0L);
        row.setItemPublishCount(0L);
        row.setItemSoldCount(0L);
        row.setNewUserCount(0L);
        row.setActiveUserCount(0L);
        row.setViewCount(0L);
        row.setFavoriteCount(0L);
        row.setAiCallCount(0L);
        return row;
    }

    /**
     * 保留 4 位小数。
     *
     * @param value 原值
     * @return 4 位小数
     */
    private double round4(double value) {
        return BigDecimal.valueOf(value).setScale(AnalysisConstants.RATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 读缓存，Redis 异常 / 类型不符时返回 null（降级查库）。
     *
     * @param key  缓存 key
     * @param type 目标类型
     * @param <T>  类型
     * @return 缓存对象或 null
     */
    private <T> T cacheGet(String key, Class<T> type) {
        try {
            Object value = redisUtils.get(key);
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (Exception e) {
            log.debug("[analysis] Redis 读取失败，降级查库：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 写缓存，Redis 异常时忽略。
     *
     * @param key     缓存 key
     * @param value   缓存值
     * @param seconds 过期秒数
     */
    private void cacheSet(String key, Object value, long seconds) {
        try {
            redisUtils.set(key, value, seconds);
        } catch (Exception e) {
            log.debug("[analysis] Redis 写入失败，忽略：{}", e.getMessage());
        }
    }
}
