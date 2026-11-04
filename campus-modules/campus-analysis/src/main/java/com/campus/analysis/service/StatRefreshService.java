package com.campus.analysis.service;

import com.campus.analysis.dto.CategoryAggRow;
import com.campus.analysis.dto.DailyStatRow;
import com.campus.analysis.dto.FunnelRow;
import com.campus.analysis.dto.RefreshResultDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * 预聚合统计刷新服务。
 *
 * <p>统一口径：接口优先读 stat_* 预聚合表；缺失或与原始数据不一致时实时重算并回填，
 * 保证任何时候打开页面都有数。</p>
 *
 * <p>幂等：每张表按日期区间「先 delete 再 insert」，可重复执行。</p>
 *
 * @author campus-mart
 */
public interface StatRefreshService {

    /**
     * 手动重算指定区间的 4 张 stat_* 表（RFM 固定按今天重算）。
     *
     * @param startDate 开始日期，null 时默认最近 7 天（今天 - 6）
     * @param endDate   结束日期，null 时默认今天
     * @return 各表写入行数
     */
    RefreshResultDTO refreshRange(LocalDate startDate, LocalDate endDate);

    /**
     * 重算单日（定时任务使用）。
     *
     * @param date 日期
     */
    void refreshDay(LocalDate date);

    /**
     * 读取区间内按天统计行（不足一天补 0），预聚合缺失/过期时自动重算回填。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天一行，长度 = 区间天数
     */
    List<DailyStatRow> overviewRows(LocalDate startDate, LocalDate endDate);

    /**
     * 读取区间内按天漏斗（每天一行，缺失补 0），缺失/过期时自动重算回填。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天一行，长度 = 区间天数
     */
    List<FunnelRow> funnelRows(LocalDate startDate, LocalDate endDate);

    /**
     * 读取区间内一级分类成交汇总，缺失/过期时自动重算回填。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 分类汇总（按成交金额倒序）
     */
    List<CategoryAggRow> categoryRows(LocalDate startDate, LocalDate endDate);
}
