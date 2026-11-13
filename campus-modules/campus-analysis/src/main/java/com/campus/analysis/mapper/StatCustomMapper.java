package com.campus.analysis.mapper;

import com.campus.analysis.domain.entity.StatCategoryDaily;
import com.campus.analysis.domain.entity.StatOverviewDaily;
import com.campus.analysis.domain.entity.StatUserRfm;
import com.campus.analysis.dto.AiSceneRow;
import com.campus.analysis.dto.CategoryAggRow;
import com.campus.analysis.dto.DailyStatRow;
import com.campus.analysis.dto.FunnelRow;
import com.campus.analysis.dto.HotItemRow;
import com.campus.analysis.dto.OrderExportRow;
import com.campus.analysis.dto.OverviewTotalsRow;
import com.campus.analysis.dto.RecentOrderRow;
import com.campus.analysis.dto.RetentionRow;
import com.campus.analysis.dto.RfmScoredRow;
import com.campus.analysis.dto.RfmSegmentRow;
import com.campus.analysis.dto.RfmSignatureRow;
import com.campus.analysis.dto.RfmUserRow;
import com.campus.analysis.dto.SellerRankRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 自定义统计 SQL Mapper。
 *
 * <p>本模块只生成 stat_* 四张表的实体，biz_* / sys_* / ai_call_log 一律通过本 Mapper
 * 的自定义 SQL（resources/mapper/StatMapper.xml）查询，不生成跨模块实体。</p>
 *
 * <p>口径约定：</p>
 * <ul>
 *   <li>成交订单 = biz_order.status = 2（见 {@link com.campus.analysis.constant.OrderStatus#FINISHED}）；</li>
 *   <li>成交额按订单完成日期（finish_time，缺失时回退 create_time）归属；</li>
 *   <li>下单数按订单创建日期（create_time）归属；</li>
 *   <li>金额统一 BigDecimal，比例统一 Double。</li>
 * </ul>
 *
 * @author campus-mart
 */
public interface StatCustomMapper {

    // ==================================================================================
    // 一、实时聚合（数据源 biz_* / sys_* / ai_call_log）
    // ==================================================================================

    /**
     * 按天聚合的原始统计行（供回填 stat_overview_daily / 趋势图使用）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天一行，只包含有数据的日期
     */
    List<DailyStatRow> selectDailyStats(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /**
     * 按天聚合的漏斗数据（曝光 / 收藏 / 下单 / 支付 / 成交）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天一行，只包含有数据的日期
     */
    List<FunnelRow> selectFunnelDaily(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /**
     * 各一级分类成交金额 / 成交件数（实时聚合）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 按成交金额倒序
     */
    List<CategoryAggRow> selectCategoryAgg(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 留存：活跃 = 当日有下单或收藏行为的用户，按日分组计算次日 / 3 日 / 7 日留存。
     *
     * @param startDate 起始日期（含，最多近 30 天）
     * @param endDate   结束日期（含）
     * @return 每天一行
     */
    List<RetentionRow> selectRetentionRows(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 累计总览指标（GMV、订单、用户、商品、收藏、浏览、AI 调用）。
     *
     * @return 永远返回一行（无数据时为 0）
     */
    OverviewTotalsRow selectOverviewTotals();

    /**
     * 热门商品榜：按 浏览量 + 收藏量 * 3 排序。
     *
     * @param limit 条数
     * @return 商品列表
     */
    List<HotItemRow> selectHotItemRank(@Param("limit") int limit);

    /**
     * 卖家榜：按成交额倒序。
     *
     * @param limit 条数
     * @return 卖家列表
     */
    List<SellerRankRow> selectSellerRank(@Param("limit") int limit);

    /**
     * AI 使用情况：按 scene 统计调用次数 / 成功 / 降级 / 失败 / 平均耗时 / token。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 按调用次数倒序
     */
    List<AiSceneRow> selectAiUsage(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * 最新成交动态。
     *
     * @param limit 条数
     * @return 成交订单列表
     */
    List<RecentOrderRow> selectRecentFinishedOrders(@Param("limit") int limit);

    /**
     * 订单导出（按下单时间区间）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 订单明细
     */
    List<OrderExportRow> selectOrderExportRows(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // ==================================================================================
    // 二、预聚合表读取
    // ==================================================================================

    /**
     * 读取区间内 stat_overview_daily 的按天数据（缺失日期不会返回行）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 按天数据
     */
    List<DailyStatRow> selectStatOverviewRows(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 区间内原始业务数据的计数汇总（用于判断预聚合是否过期）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 键：order_count / finish_count / pay_count / favorite_count / view_count /
     *         ai_call_count / new_user_count / item_publish_count / order_gmv
     */
    java.util.Map<String, Object> selectRangeTotals(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 按天 + 一级分类的成交数据（回填 stat_category_daily）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天每个一级分类一行
     */
    List<com.campus.analysis.dto.CategoryDailyRow> selectCategoryOrderDaily(@Param("startDate") LocalDate startDate,
                                                                            @Param("endDate") LocalDate endDate);

    /**
     * 按天 + 一级分类的商品发布数（回填 stat_category_daily.item_count）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每天每个一级分类一行
     */
    List<com.campus.analysis.dto.CategoryItemRow> selectCategoryItemDaily(@Param("startDate") LocalDate startDate,
                                                                          @Param("endDate") LocalDate endDate);

    /**
     * 读取区间内 stat_funnel_daily 的按天数据。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 按天数据
     */
    List<FunnelRow> selectStatFunnelRows(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    /**
     * 读取区间内 stat_category_daily 的分类汇总。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 分类汇总行
     */
    List<CategoryAggRow> selectStatCategoryAgg(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * 统计区间内 stat_category_daily 覆盖的天数（用于判断是否需要回填）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 覆盖天数
     */
    int countStatCategoryDays(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    // ==================================================================================
    // 三、预聚合表写入（幂等：先删后插 / ON DUPLICATE KEY UPDATE）
    // ==================================================================================

    /**
     * 删除区间内 stat_overview_daily（幂等重算用）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 删除行数
     */
    int deleteOverviewDaily(@Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    /**
     * 批量写入 stat_overview_daily。
     *
     * @param list 按天统计
     * @return 写入行数
     */
    int insertOverviewDailyBatch(@Param("list") List<StatOverviewDaily> list);

    /**
     * 删除区间内 stat_funnel_daily（幂等重算用）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 删除行数
     */
    int deleteFunnelDaily(@Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * 批量写入 stat_funnel_daily。
     *
     * @param list 按天漏斗数据
     * @return 写入行数
     */
    int insertFunnelDailyBatch(@Param("list") List<FunnelRow> list);

    /**
     * 删除区间内 stat_category_daily（幂等重算用）。
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 删除行数
     */
    int deleteCategoryDaily(@Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    /**
     * 批量写入 stat_category_daily。
     *
     * @param list 分类日数据
     * @return 写入行数
     */
    int insertCategoryDailyBatch(@Param("list") List<StatCategoryDaily> list);

    // ==================================================================================
    // 四、RFM 用户分层
    // ==================================================================================

    /**
     * 有成交的用户 RFM 原始数据 + 分位数得分（NTILE(5)，1-5 分）。
     *
     * <p>只统计 status = 2 的订单；R = 最近一次成交距今天数，F = 成交次数，M = 成交金额。</p>
     *
     * @return 有成交用户列表
     */
    List<RfmScoredRow> selectRfmScoredUsers();

    /**
     * 无成交用户的基线行（用于把无成交学生用户标记为「流失」）。
     *
     * <p>返回一行，recencyDays = 999、frequency = 0、monetary = 0、r/f/m 均为 1；
     * 无学生用户时该行各计数字段为 0。</p>
     *
     * @return 基线行
     */
    RfmScoredRow selectRfmBaseline();

    /**
     * 无成交的学生用户（用于标记为「流失」）。
     *
     * @return 用户列表（frequency = 0）
     */
    List<RfmScoredRow> selectRfmChurnUsers();

    /**
     * 删除某日 stat_user_rfm（幂等重算用）。
     *
     * @param calcDate 计算日期
     * @return 删除行数
     */
    int deleteUserRfm(@Param("calcDate") LocalDate calcDate);

    /**
     * 批量写入 stat_user_rfm。
     *
     * @param list 用户分层数据
     * @return 写入行数
     */
    int insertUserRfmBatch(@Param("list") List<StatUserRfm> list);

    /**
     * 清理历史 RFM 数据（保留最近 N 天，避免表无限增长）。
     *
     * @param calcDate 早于该日期的数据被删除
     * @return 删除行数
     */
    int deleteUserRfmBefore(@Param("calcDate") LocalDate calcDate);

    /**
     * 读取某日 RFM 明细（分页）。
     *
     * @param calcDate 计算日期
     * @param segment  人群（null 表示全部）
     * @param offset   偏移量
     * @param limit    条数
     * @return 明细
     */
    List<RfmUserRow> selectRfmRows(@Param("calcDate") LocalDate calcDate,
                                   @Param("segment") String segment,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计某日 RFM 明细总数。
     *
     * @param calcDate 计算日期
     * @param segment  人群（null 表示全部）
     * @return 总数
     */
    long countRfmRows(@Param("calcDate") LocalDate calcDate,
                      @Param("segment") String segment);

    /**
     * 某日各人群人数 / 金额（用于分层汇总）。
     *
     * @param calcDate 计算日期
     * @return 每个出现的人群一行
     */
    List<RfmSegmentRow> selectRfmSegments(@Param("calcDate") LocalDate calcDate);

    /**
     * 某日 RFM 总人数 / 总金额（用于计算占比分母；无数据时返回 null 字段）。
     *
     * @param calcDate 计算日期
     * @return 汇总
     */
    RfmSignatureRow selectRfmSignature(@Param("calcDate") LocalDate calcDate);

    /**
     * 某日 RFM 明细中出现的所有人群名称（用于补齐 8 类人群零值）。
     *
     * @param calcDate 计算日期
     * @return 人群列表
     */
    List<RfmSegmentRow> selectRfmSegmentRows(@Param("calcDate") LocalDate calcDate);
}
