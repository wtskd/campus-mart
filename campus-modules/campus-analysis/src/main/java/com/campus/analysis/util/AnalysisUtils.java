package com.campus.analysis.util;

import com.campus.analysis.constant.AnalysisConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 分析模块工具方法：金额 / 比例 / 日期统一格式化。
 *
 * <p>约定：金额保留 2 位小数（BigDecimal），比例保留 4 位小数（Double，0.1234 = 12.34%）。</p>
 *
 * @author campus-mart
 */
public final class AnalysisUtils {

    /** 日期格式 yyyy-MM-dd */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 日期时间格式 yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AnalysisUtils() {
    }

    /**
     * 业务时区今天（Asia/Shanghai）。
     *
     * @return 今天
     */
    public static LocalDate today() {
        return LocalDate.now(AnalysisConstants.ZONE);
    }

    /**
     * 业务时区当前时间。
     *
     * @return 当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(AnalysisConstants.ZONE);
    }

    /**
     * 金额规整：null -> 0.00，保留 2 位小数。
     *
     * @param value 金额
     * @return 规整后的金额
     */
    public static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(AnalysisConstants.MONEY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(AnalysisConstants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 数值转金额。
     *
     * @param value 数值
     * @return 金额
     */
    public static BigDecimal money(Number value) {
        if (value == null) {
            return money((BigDecimal) null);
        }
        return money(new BigDecimal(value.toString()));
    }

    /**
     * 求和后规整为金额。
     *
     * @param values 金额
     * @return 合计
     */
    public static BigDecimal sumMoney(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null) {
                    total = total.add(v);
                }
            }
        }
        return money(total);
    }

    /**
     * 比例：part / total，保留 4 位小数；total <= 0 时返回 0。
     *
     * @param part  分子
     * @param total 分母
     * @return 比例
     */
    public static Double rate(long part, long total) {
        return rate(BigDecimal.valueOf(part), BigDecimal.valueOf(total));
    }

    /**
     * 比例：part / total，保留 4 位小数；total <= 0 时返回 0。
     *
     * @param part  分子
     * @param total 分母
     * @return 比例
     */
    public static Double rate(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0D;
        }
        return part.divide(total, AnalysisConstants.RATE_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 同比 / 环比：今日与昨日对比，昨日为 0 时今日有值记 1.0。
     *
     * @param current  今日值
     * @param previous 昨日值
     * @return 变化率
     */
    public static Double compare(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current == null ? BigDecimal.ZERO : current;
        BigDecimal pre = previous == null ? BigDecimal.ZERO : previous;
        if (pre.compareTo(BigDecimal.ZERO) == 0) {
            return cur.compareTo(BigDecimal.ZERO) > 0 ? 1D : 0D;
        }
        return cur.subtract(pre).divide(pre, AnalysisConstants.RATE_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 平均客单价：金额 / 成交单数，保留 2 位小数。
     *
     * @param gmv         金额
     * @param orderCount  成交单数
     * @return 平均客单价
     */
    public static BigDecimal avgOrderAmount(BigDecimal gmv, long orderCount) {
        if (orderCount <= 0) {
            return money((BigDecimal) null);
        }
        return money((gmv == null ? BigDecimal.ZERO : gmv).divide(BigDecimal.valueOf(orderCount),
                AnalysisConstants.MONEY_SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Long 空值转 0。
     *
     * @param value 数值
     * @return 非空数值
     */
    public static long nvl(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * Integer 空值转 0。
     *
     * @param value 数值
     * @return 非空数值
     */
    public static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Object 转 long（MyBatis Map 结果使用）。
     *
     * @param value 数值
     * @return 数值
     */
    public static long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * Object 转 BigDecimal。
     *
     * @param value 数值
     * @return 金额
     */
    public static BigDecimal toMoney(Object value) {
        return value instanceof Number n ? money(n) : money((BigDecimal) null);
    }

    /**
     * LocalDate 格式化。
     *
     * @param date 日期
     * @return yyyy-MM-dd
     */
    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    /**
     * LocalDateTime 格式化。
     *
     * @param dateTime 时间
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATETIME_FORMATTER);
    }
}
