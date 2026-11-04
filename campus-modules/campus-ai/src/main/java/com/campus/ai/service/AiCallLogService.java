package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.domain.entity.AiCallLog;
import com.campus.ai.mapper.AiCallLogMapper;
import com.campus.ai.provider.AiResponse;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * AI 调用日志（ai_call_log）写入与分页查询
 *
 * <p>status：'0' 成功、'2' 降级兜底（目标 provider 失败后由 mock 兜住）、'1' 失败。
 * 请求摘要会做手机号/微信号脱敏后截断到 500 字符（ai_call_log.request_summary 长度）。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    private static final int SUMMARY_MAX = 500;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern PHONE = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    private static final Pattern CONTACT = Pattern.compile(
            "(?i)(微信|weixin|vx|v信|扣扣|qq)\\s*[:：]?\\s*[a-zA-Z0-9_-]{5,20}");

    private final AiCallLogMapper callLogMapper;

    /**
     * 写一次调用日志（不抛异常，不能影响主流程）
     */
    public void write(String scene, String templateCode, String requestSummary,
                      AiResponse response, String status, String errorMsg) {
        if (response == null) {
            return;
        }
        try {
            AiCallLog row = new AiCallLog();
            row.setUserId(SecurityUtils.getUserId());
            row.setScene(truncate(scene, 32));
            row.setTemplateCode(truncate(templateCode, 64));
            row.setProvider(truncate(response.getProvider(), 16));
            row.setModel(truncate(response.getModel(), 64));
            row.setRequestSummary(sanitize(requestSummary));
            row.setPromptTokens(response.getPromptTokens() == null ? 0 : response.getPromptTokens());
            row.setCompletionTokens(response.getCompletionTokens() == null ? 0 : response.getCompletionTokens());
            row.setTotalTokens(response.totalTokens());
            row.setCostMs(response.getCostMs());
            row.setStatus(status);
            row.setErrorMsg(truncate(errorMsg, 500));
            callLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("写入 AI 调用日志失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /**
     * 调用日志分页
     *
     * @param scene    场景（copywrite/chat/audit/summary/recommend）
     * @param provider remote/local/mock/rule
     * @param status   0成功 2降级兜底 1失败
     * @param beginTime yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
     * @param endTime   同上
     */
    public PageResult<AiCallLog> page(PageQuery query, String scene, String provider,
                                      String status, String beginTime, String endTime) {
        LambdaQueryWrapper<AiCallLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(scene), AiCallLog::getScene, scene)
                .eq(StringUtils.hasText(provider), AiCallLog::getProvider, provider)
                .eq(StringUtils.hasText(status), AiCallLog::getStatus, status);
        LocalDateTime begin = parseTime(beginTime, false);
        LocalDateTime end = parseTime(endTime, true);
        wrapper.ge(begin != null, AiCallLog::getCreateTime, begin)
                .le(end != null, AiCallLog::getCreateTime, end)
                .orderByDesc(AiCallLog::getId);
        Page<AiCallLog> page = callLogMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    private LocalDateTime parseTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                LocalDate date = LocalDate.parse(text);
                return endOfDay ? date.plusDays(1).atStartOfDay().minusSeconds(1) : date.atStartOfDay();
            }
            String normalized = text.length() == 16 ? text + ":00" : text;
            return LocalDateTime.parse(normalized, DATE_TIME);
        } catch (Exception e) {
            log.debug("忽略非法的日志时间筛选参数: {}", value);
            return null;
        }
    }

    private String sanitize(String summary) {
        if (summary == null) {
            return null;
        }
        String text = PHONE.matcher(summary).replaceAll("$1****$2");
        text = CONTACT.matcher(text).replaceAll("***");
        return truncate(text.replaceAll("\\s+", " ").trim(), SUMMARY_MAX);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
