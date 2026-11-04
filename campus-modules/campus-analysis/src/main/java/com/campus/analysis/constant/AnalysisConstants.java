package com.campus.analysis.constant;

import java.time.ZoneId;

/**
 * 分析模块常量
 *
 * @author campus-mart
 */
public final class AnalysisConstants {

    private AnalysisConstants() {
    }

    /** 业务时区 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 总览缓存 key */
    public static final String CACHE_OVERVIEW = "analysis:overview";

    /** 大屏缓存 key */
    public static final String CACHE_DASHBOARD = "analysis:dashboard";

    /** 总览缓存秒数 */
    public static final long CACHE_OVERVIEW_SECONDS = 60L;

    /** 大屏缓存秒数 */
    public static final long CACHE_DASHBOARD_SECONDS = 30L;

    /** 预聚合补算去重标记前缀（避免空数据日反复回算） */
    public static final String CACHE_ENSURE_PREFIX = "analysis:stat:ensure:";

    /** 预聚合补算去重标记秒数 */
    public static final long CACHE_ENSURE_SECONDS = 300L;

    /** 普通学生用户类型（sys_user.user_type） */
    public static final String USER_TYPE_STUDENT = "01";

    /** RFM 分层：流失（无成交用户） */
    public static final String SEGMENT_LOST = "流失";

    /** RFM 高分段阈值：得分 >= 4 视为「高」 */
    public static final int RFM_HIGH_SCORE = 4;

    /** RFM 无成交用户的 R 值（默认 999 天） */
    public static final int RFM_MAX_RECENCY = 999;

    /** 金额保留小数位 */
    public static final int MONEY_SCALE = 2;

    /** 比例保留小数位 */
    public static final int RATE_SCALE = 4;

    /** 支持的趋势天数：7 / 14 / 30 */
    public static final int DEFAULT_TREND_DAYS = 7;

    /** 默认分类/留存统计天数 */
    public static final int DEFAULT_DAYS = 30;
}
