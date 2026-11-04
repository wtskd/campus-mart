package com.campus.system.util;

import cn.hutool.core.util.StrUtil;
import com.campus.common.core.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 查询时间区间解析工具。
 *
 * <p>前端可能传 {@code beginTime=2026-10-27}（日期）也可能传完整时间，
 * 还可能按 RuoYi 习惯传 {@code params[beginTime]}。这里统一收口，避免每个日志查询各写一套解析。</p>
 *
 * @author campus-mart
 */
public final class QueryTimeUtils {

    private QueryTimeUtils() {
    }

    /** 区间起点：只传日期时按当天 00:00:00 处理 */
    public static LocalDateTime begin(String text) {
        LocalDateTime time = parse(text);
        if (time == null) {
            return null;
        }
        return text.trim().length() <= 10 ? time.toLocalDate().atStartOfDay() : time;
    }

    /** 区间终点：只传日期时按当天 23:59:59 处理，否则“今天到明天”会查不到今天的数据 */
    public static LocalDateTime end(String text) {
        LocalDateTime time = parse(text);
        if (time == null) {
            return null;
        }
        return text.trim().length() <= 10 ? time.toLocalDate().atTime(23, 59, 59) : time;
    }

    /** 同时兼容 {@code ?beginTime=} 与 {@code ?params[beginTime]=} 两种传参方式 */
    public static String pick(Map<String, Object> params, String key, String explicit) {
        if (StrUtil.isNotBlank(explicit)) {
            return explicit;
        }
        if (params == null) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static LocalDateTime parse(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                return java.time.LocalDate.parse(value).atStartOfDay();
            }
            String iso = value.replace(' ', 'T');
            // 兼容 yyyy-MM-ddTHH:mm 这种缺秒的写法
            if (iso.length() == 16) {
                iso = iso + ":00";
            }
            return LocalDateTime.parse(iso);
        } catch (Exception e) {
            throw new ServiceException(400, "时间格式不正确：" + text);
        }
    }
}
