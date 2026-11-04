package com.campus.analysis.service;

import com.campus.analysis.dto.AiUsageDTO;
import com.campus.analysis.dto.CategoryStatDTO;
import com.campus.analysis.dto.DashboardDTO;
import com.campus.analysis.dto.FunnelDTO;
import com.campus.analysis.dto.HotItemDTO;
import com.campus.analysis.dto.OverviewDTO;
import com.campus.analysis.dto.RetentionDTO;
import com.campus.analysis.dto.RfmDTO;
import com.campus.analysis.dto.SellerRankDTO;
import com.campus.analysis.dto.TrendDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * 数据分析查询服务（前端 /analysis/** 接口）。
 *
 * <p>读取顺序：Redis 缓存 → stat_* 预聚合表 → 实时聚合（并回填预聚合）。</p>
 *
 * @author campus-mart
 */
public interface AnalysisService {

    /**
     * 总览卡片（今日 / 昨日 / 近 7 日 / 累计）。
     *
     * @return 总览
     */
    OverviewDTO overview();

    /**
     * 趋势折线：GMV、订单数、新增用户、新增商品。
     *
     * @param days 天数（默认 7，支持 7 / 14 / 30）
     * @return 趋势
     */
    TrendDTO trend(int days);

    /**
     * 一级分类成交占比（饼图）。
     *
     * @param days 天数（默认 30）
     * @return 分类统计
     */
    List<CategoryStatDTO> category(int days);

    /**
     * 转化漏斗：曝光 → 收藏 → 下单 → 成交。
     *
     * @param days 天数（默认 7）
     * @return 漏斗
     */
    FunnelDTO funnel(int days);

    /**
     * 用户留存：次日 / 3 日 / 7 日（只算近 30 天）。
     *
     * @param days 天数（默认 7）
     * @return 留存
     */
    RetentionDTO retention(int days);

    /**
     * RFM 用户分层。
     *
     * @param page  页码
     * @param limit 每页条数
     * @return 分层汇总 + 明细
     */
    RfmDTO rfm(int page, int limit);

    /**
     * 热门商品榜。
     *
     * @param limit 条数
     * @return 商品榜
     */
    List<HotItemDTO> hotItem(int limit);

    /**
     * 卖家榜。
     *
     * @param limit 条数
     * @return 卖家榜
     */
    List<SellerRankDTO> sellerRank(int limit);

    /**
     * AI 使用情况（按场景）。
     *
     * @param days 天数（默认 7）
     * @return AI 使用统计
     */
    AiUsageDTO aiUsage(int days);

    /**
     * 数据大屏聚合（overview + trend + category + funnel + hotItem + aiUsage + 最新成交动态）。
     *
     * @return 大屏数据
     */
    DashboardDTO dashboard();

    /**
     * 导出订单 CSV（UTF-8 BOM）。
     *
     * @param startDate 起始日期（含），默认近 7 天
     * @param endDate   结束日期（含），默认今天
     * @return CSV 文本
     */
    String exportOrderCsv(LocalDate startDate, LocalDate endDate);
}
