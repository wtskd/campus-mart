package com.campus.analysis.service.impl;

import com.campus.analysis.constant.AnalysisConstants;
import com.campus.analysis.domain.entity.StatCategoryDaily;
import com.campus.analysis.domain.entity.StatFunnelDaily;
import com.campus.analysis.domain.entity.StatOverviewDaily;
import com.campus.analysis.dto.CategoryAggRow;
import com.campus.analysis.dto.CategoryDailyRow;
import com.campus.analysis.dto.CategoryItemRow;
import com.campus.analysis.dto.DailyStatRow;
import com.campus.analysis.dto.FunnelRow;
import com.campus.analysis.dto.RefreshResultDTO;
import com.campus.analysis.mapper.StatCustomMapper;
import com.campus.analysis.service.RfmService;
import com.campus.analysis.service.StatRefreshService;
import com.campus.analysis.util.AnalysisUtils;
import com.campus.common.utils.RedisUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预聚合统计刷新服务实现。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>接口读 stat_* 表；读取时先做「口径一致性校验」（与 biz_* 实时聚合计数比对），
 *       不一致或缺失则实时重算并回填，保证页面任何时候都有数；</li>
 *   <li>重算一律「先 delete 区间再 insert」，天然幂等，可重复执行；</li>
 *   <li>空数据日也会写入 0 值行，保证趋势图日期连续；</li>
 *   <li>回填失败不抛异常：接口降级为实时聚合，演示环境不因统计表问题 500。</li>
 * </ul>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatRefreshServiceImpl implements StatRefreshService {

    /** 单次重算的最大区间天数，避免误传超大区间拖垮数据库 */
    private static final int MAX_RANGE_DAYS = 366;

    private final StatCustomMapper statMapper;

    private final RfmService rfmService;

    private final RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshResultDTO refreshRange(LocalDate startDate, LocalDate endDate) {
        long begin = System.currentTimeMillis();
        LocalDate end = endDate == null ? AnalysisUtils.today() : endDate;
        LocalDate start = startDate == null ? end.minusDays(AnalysisConstants.DEFAULT_TREND_DAYS - 1L) : startDate;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            start = end.minusDays(MAX_RANGE_DAYS);
        }

        int overviewRows = refreshOverview(start, end);
        int categoryRows = refreshCategory(start, end);
        int funnelRows = refreshFunnel(start, end);
        // RFM 是「截至今天」的用户快照，固定按今天重算
        int rfmRows = rfmService.computeRfm(AnalysisUtils.today()).size();

        RefreshResultDTO result = new RefreshResultDTO();
        result.setStartDate(AnalysisUtils.format(start));
        result.setEndDate(AnalysisUtils.format(end));
        result.setOverviewRows(overviewRows);
        result.setCategoryRows(categoryRows);
        result.setFunnelRows(funnelRows);
        result.setRfmRows(rfmRows);
        result.setTotalRows(overviewRows + categoryRows + funnelRows + rfmRows);
        result.setCostMs(System.currentTimeMillis() - begin);
        result.setMessage("重算完成");
        log.info("[analysis] 统计重算完成 {} ~ {}：overview={} category={} funnel={} rfm={} 耗时 {}ms",
                start, end, overviewRows, categoryRows, funnelRows, rfmRows, result.getCostMs());
        return result;
    }

    @Override
    public void refreshDay(LocalDate date) {
        LocalDate day = date == null ? AnalysisUtils.today().minusDays(1) : date;
        refreshRange(day, day);
    }

    @Override
    public List<DailyStatRow> overviewRows(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate;
        LocalDate end = endDate;
        List<DailyStatRow> filled = fillDaily(statMapper.selectStatOverviewRows(start, end), start, end);
        if (!isOverviewStale(filled, start, end)) {
            return filled;
        }
        // 去重标记：避免「确实没有数据」的区间每次请求都全量重算
        String flag = AnalysisConstants.CACHE_ENSURE_PREFIX + "overview:" + start + ":" + end;
        if (cacheHasKey(flag)) {
            return filled;
        }
        try {
            refreshOverview(start, end);
        } catch (Exception e) {
            log.warn("[analysis] stat_overview_daily 回填失败，降级为实时聚合：{}", e.getMessage());
            return fillDaily(statMapper.selectDailyStats(start, end), start, end);
        }
        cacheSet(flag, 1, AnalysisConstants.CACHE_ENSURE_SECONDS);
        return fillDaily(statMapper.selectStatOverviewRows(start, end), start, end);
    }

    @Override
    public List<FunnelRow> funnelRows(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate;
        LocalDate end = endDate;
        List<FunnelRow> filled = fillFunnel(statMapper.selectStatFunnelRows(start, end), start, end);
        if (!isFunnelStale(filled, start, end)) {
            return filled;
        }
        String flag = AnalysisConstants.CACHE_ENSURE_PREFIX + "funnel:" + start + ":" + end;
        if (cacheHasKey(flag)) {
            return filled;
        }
        try {
            refreshFunnel(start, end);
        } catch (Exception e) {
            log.warn("[analysis] stat_funnel_daily 回填失败，降级为实时聚合：{}", e.getMessage());
            return fillFunnel(statMapper.selectFunnelDaily(start, end), start, end);
        }
        cacheSet(flag, 1, AnalysisConstants.CACHE_ENSURE_SECONDS);
        return fillFunnel(statMapper.selectStatFunnelRows(start, end), start, end);
    }

    @Override
    public List<CategoryAggRow> categoryRows(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate;
        LocalDate end = endDate;
        List<CategoryAggRow> statRows = statMapper.selectStatCategoryAgg(start, end);
        List<CategoryAggRow> liveRows = statMapper.selectCategoryAgg(start, end);
        if (sameCategory(statRows, liveRows)) {
            return statRows;
        }
        String flag = AnalysisConstants.CACHE_ENSURE_PREFIX + "category:" + start + ":" + end;
        if (cacheHasKey(flag)) {
            return statRows.isEmpty() ? liveRows : statRows;
        }
        try {
            refreshCategory(start, end);
        } catch (Exception e) {
            log.warn("[analysis] stat_category_daily 回填失败，降级为实时聚合：{}", e.getMessage());
            return liveRows;
        }
        cacheSet(flag, 1, AnalysisConstants.CACHE_ENSURE_SECONDS);
        List<CategoryAggRow> refreshed = statMapper.selectStatCategoryAgg(start, end);
        return refreshed.isEmpty() ? liveRows : refreshed;
    }

    // ==================================================================================
    // 单表重算
    // ==================================================================================

    /**
     * 重算 stat_overview_daily，返回写入行数。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 写入行数
     */
    private int refreshOverview(LocalDate start, LocalDate end) {
        List<DailyStatRow> rows = fillDaily(statMapper.selectDailyStats(start, end), start, end);
        statMapper.deleteOverviewDaily(start, end);
        List<StatOverviewDaily> list = new ArrayList<>(rows.size());
        for (DailyStatRow row : rows) {
            StatOverviewDaily entity = new StatOverviewDaily();
            entity.setStatDate(row.getStatDate());
            entity.setGmv(AnalysisUtils.money(row.getGmv()));
            entity.setOrderCount(intOf(row.getOrderCount()));
            entity.setFinishOrderCount(intOf(row.getFinishOrderCount()));
            entity.setCancelOrderCount(intOf(row.getCancelOrderCount()));
            entity.setItemPublishCount(intOf(row.getItemPublishCount()));
            entity.setItemSoldCount(intOf(row.getItemSoldCount()));
            entity.setNewUserCount(intOf(row.getNewUserCount()));
            entity.setActiveUserCount(intOf(row.getActiveUserCount()));
            entity.setViewCount(intOf(row.getViewCount()));
            entity.setFavoriteCount(intOf(row.getFavoriteCount()));
            entity.setAiCallCount(intOf(row.getAiCallCount()));
            list.add(entity);
        }
        if (list.isEmpty()) {
            return 0;
        }
        return statMapper.insertOverviewDailyBatch(list);
    }

    /**
     * 重算 stat_category_daily，返回写入行数。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 写入行数
     */
    private int refreshCategory(LocalDate start, LocalDate end) {
        List<CategoryDailyRow> orderRows = statMapper.selectCategoryOrderDaily(start, end);
        List<CategoryItemRow> itemRows = statMapper.selectCategoryItemDaily(start, end);
        statMapper.deleteCategoryDaily(start, end);
        Map<String, StatCategoryDaily> merged = new LinkedHashMap<>();
        for (CategoryDailyRow row : orderRows) {
            StatCategoryDaily entity = merged.computeIfAbsent(key(row.getStatDate(), row.getCategoryId()),
                    k -> newCategory(row.getStatDate(), row.getCategoryId()));
            entity.setOrderCount(intOf(row.getOrderCount()));
            entity.setGmv(AnalysisUtils.money(row.getGmv()));
        }
        for (CategoryItemRow row : itemRows) {
            StatCategoryDaily entity = merged.computeIfAbsent(key(row.getStatDate(), row.getCategoryId()),
                    k -> newCategory(row.getStatDate(), row.getCategoryId()));
            entity.setItemCount(intOf(row.getItemCount()));
        }
        if (merged.isEmpty()) {
            return 0;
        }
        return statMapper.insertCategoryDailyBatch(new ArrayList<>(merged.values()));
    }

    /**
     * 重算 stat_funnel_daily，返回写入行数。
     *
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 写入行数
     */
    private int refreshFunnel(LocalDate start, LocalDate end) {
        List<FunnelRow> rows = fillFunnel(statMapper.selectFunnelDaily(start, end), start, end);
        statMapper.deleteFunnelDaily(start, end);
        if (rows.isEmpty()) {
            return 0;
        }
        return statMapper.insertFunnelDailyBatch(rows);
    }

    // ==================================================================================
    // 过期判断
    // ==================================================================================

    /**
     * 预聚合总览是否过期：与 biz_* 实时聚合计数比对（比时间戳更准）。
     *
     * @param statRows 已补齐日期的统计行
     * @param start    起始日期（含）
     * @param end      结束日期（含）
     * @return true 表示需要重算
     */
    private boolean isOverviewStale(List<DailyStatRow> statRows, LocalDate start, LocalDate end) {
        Map<String, Object> live = statMapper.selectRangeTotals(start, end);
        if (live == null) {
            return true;
        }
        long statOrder = 0L;
        long statFinish = 0L;
        long statNewUser = 0L;
        long statItem = 0L;
        long statFavorite = 0L;
        long statView = 0L;
        long statAi = 0L;
        BigDecimal statGmv = BigDecimal.ZERO;
        for (DailyStatRow row : statRows) {
            statOrder += AnalysisUtils.nvl(row.getOrderCount());
            statFinish += AnalysisUtils.nvl(row.getFinishOrderCount());
            statNewUser += AnalysisUtils.nvl(row.getNewUserCount());
            statItem += AnalysisUtils.nvl(row.getItemPublishCount());
            statFavorite += AnalysisUtils.nvl(row.getFavoriteCount());
            statView += AnalysisUtils.nvl(row.getViewCount());
            statAi += AnalysisUtils.nvl(row.getAiCallCount());
            statGmv = statGmv.add(AnalysisUtils.money(row.getGmv()));
        }
        return statOrder != AnalysisUtils.toLong(live.get("order_count"))
                || statFinish != AnalysisUtils.toLong(live.get("finish_count"))
                || statNewUser != AnalysisUtils.toLong(live.get("new_user_count"))
                || statItem != AnalysisUtils.toLong(live.get("item_publish_count"))
                || statFavorite != AnalysisUtils.toLong(live.get("favorite_count"))
                || statView != AnalysisUtils.toLong(live.get("view_count"))
                || statAi != AnalysisUtils.toLong(live.get("ai_call_count"))
                || AnalysisUtils.money(statGmv).compareTo(AnalysisUtils.toMoney(live.get("order_gmv"))) != 0;
    }

    /**
     * 漏斗预聚合是否过期。
     *
     * @param statRows 已补齐日期的漏斗行
     * @param start    起始日期（含）
     * @param end      结束日期（含）
     * @return true 表示需要重算
     */
    private boolean isFunnelStale(List<FunnelRow> statRows, LocalDate start, LocalDate end) {
        Map<String, Object> live = statMapper.selectRangeTotals(start, end);
        if (live == null) {
            return true;
        }
        long statView = 0L;
        long statFavorite = 0L;
        long statOrder = 0L;
        long statFinish = 0L;
        for (FunnelRow row : statRows) {
            statView += AnalysisUtils.nvl(row.getViewCount());
            statFavorite += AnalysisUtils.nvl(row.getFavoriteCount());
            statOrder += AnalysisUtils.nvl(row.getOrderCount());
            statFinish += AnalysisUtils.nvl(row.getFinishCount());
        }
        return statView != AnalysisUtils.toLong(live.get("view_count"))
                || statFavorite != AnalysisUtils.toLong(live.get("favorite_count"))
                || statOrder != AnalysisUtils.toLong(live.get("order_count"))
                || statFinish != AnalysisUtils.toLong(live.get("finish_count"));
    }

    /**
     * 分类预聚合是否与实时聚合一致。
     *
     * @param statRows 预聚合结果
     * @param liveRows 实时聚合结果
     * @return true 表示一致
     */
    private boolean sameCategory(List<CategoryAggRow> statRows, List<CategoryAggRow> liveRows) {
        if ((statRows == null || statRows.isEmpty()) && (liveRows == null || liveRows.isEmpty())) {
            return true;
        }
        return sumCategory(statRows).equals(sumCategory(liveRows));
    }

    /**
     * 分类汇总求和（成交单数 + 成交金额）。
     *
     * @param rows 分类行
     * @return 计数 / 金额
     */
    private CategorySum sumCategory(List<CategoryAggRow> rows) {
        long count = 0L;
        BigDecimal gmv = BigDecimal.ZERO;
        if (rows != null) {
            for (CategoryAggRow row : rows) {
                count += AnalysisUtils.nvl(row.getOrderCount());
                gmv = gmv.add(AnalysisUtils.money(row.getGmv()));
            }
        }
        return new CategorySum(count, AnalysisUtils.money(gmv));
    }

    // ==================================================================================
    // 补齐日期
    // ==================================================================================

    /**
     * 按日期补齐（缺失日期补 0 值行），保证趋势图连续。
     *
     * @param rows  查询结果（可能稀疏）
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 每天一行
     */
    private List<DailyStatRow> fillDaily(List<DailyStatRow> rows, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyStatRow> map = new HashMap<>();
        if (rows != null) {
            for (DailyStatRow row : rows) {
                if (row.getStatDate() != null) {
                    map.put(row.getStatDate(), row);
                }
            }
        }
        List<DailyStatRow> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DailyStatRow row = map.get(d);
            if (row == null) {
                row = new DailyStatRow();
                row.setStatDate(d);
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
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 漏斗按日期补齐（缺失日期补 0）。
     *
     * @param rows  查询结果（可能稀疏）
     * @param start 起始日期（含）
     * @param end   结束日期（含）
     * @return 每天一行
     */
    private List<FunnelRow> fillFunnel(List<FunnelRow> rows, LocalDate start, LocalDate end) {
        Map<LocalDate, FunnelRow> map = new HashMap<>();
        if (rows != null) {
            for (FunnelRow row : rows) {
                if (row.getStatDate() != null) {
                    map.put(row.getStatDate(), row);
                }
            }
        }
        List<FunnelRow> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            FunnelRow row = map.get(d);
            if (row == null) {
                row = new FunnelRow();
                row.setStatDate(d);
                row.setViewCount(0L);
                row.setFavoriteCount(0L);
                row.setOrderCount(0L);
                row.setPayCount(0L);
                row.setFinishCount(0L);
            }
            result.add(row);
        }
        return result;
    }

    // ==================================================================================
    // 小工具
    // ==================================================================================

    private static Integer intOf(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static StatCategoryDaily newCategory(LocalDate date, Long categoryId) {
        StatCategoryDaily entity = new StatCategoryDaily();
        entity.setStatDate(date);
        entity.setCategoryId(categoryId);
        entity.setItemCount(0);
        entity.setOrderCount(0);
        entity.setGmv(BigDecimal.ZERO);
        return entity;
    }

    private static String key(LocalDate date, Long categoryId) {
        return date + "#" + categoryId;
    }

    /**
     * 缓存存在判断（Redis 不可用时降级为 false，不影响接口）。
     *
     * @param key 缓存 key
     * @return true 表示存在
     */
    private boolean cacheHasKey(String key) {
        try {
            return redisUtils.hasKey(key);
        } catch (Exception e) {
            log.debug("[analysis] Redis 不可用，跳过缓存判断：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 写缓存（Redis 不可用时忽略）。
     *
     * @param key     缓存 key
     * @param value   缓存值
     * @param seconds 过期秒数
     */
    private void cacheSet(String key, Object value, long seconds) {
        try {
            redisUtils.set(key, value, seconds);
        } catch (Exception e) {
            log.debug("[analysis] Redis 不可用，跳过写缓存：{}", e.getMessage());
        }
    }

    /**
     * 分类汇总计数（用于一致性比较）。
     *
     * @param count 成交单数
     * @param gmv   成交金额
     */
    private record CategorySum(long count, BigDecimal gmv) {
    }
}
