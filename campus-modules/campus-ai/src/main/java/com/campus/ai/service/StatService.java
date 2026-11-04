package com.campus.ai.service;

import com.campus.ai.domain.dto.AiStatRow;
import com.campus.ai.domain.dto.StatOverviewVO;
import com.campus.ai.mapper.AiStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 使用概览统计（管理端）
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    /** 历史日志没有 token 时的单次调用估算值 */
    private static final long TOKEN_PER_CALL_ESTIMATE = 380L;

    private static final int MAX_DAYS = 90;

    private final AiStatMapper statMapper;

    public StatOverviewVO overview(Integer days) {
        int span = days == null || days <= 0 ? 7 : Math.min(days, MAX_DAYS);
        LocalDate beginDate = LocalDate.now().minusDays(span - 1L);
        LocalDateTime begin = beginDate.atStartOfDay();

        AiStatRow.Total total = null;
        List<AiStatRow.Scene> sceneRows = new ArrayList<>();
        List<AiStatRow.Provider> providerRows = new ArrayList<>();
        List<AiStatRow.Day> dayRows = new ArrayList<>();
        try {
            total = statMapper.overview(begin);
            sceneRows = statMapper.sceneStats(begin);
            providerRows = statMapper.providerStats(begin);
            dayRows = statMapper.dayStats(begin);
        } catch (Exception e) {
            log.warn("AI 统计查询失败，返回空概览：{}", e.getMessage());
        }

        long totalCalls = total == null ? 0L : nz(total.getTotalCalls());
        long successCalls = total == null ? 0L : nz(total.getSuccessCalls());
        long fallbackCalls = total == null ? 0L : nz(total.getFallbackCalls());
        long failCalls = total == null ? 0L : nz(total.getFailCalls());
        double avgCostMs = total == null || total.getAvgCostMs() == null ? 0D : total.getAvgCostMs();
        long promptTokens = total == null ? 0L : nz(total.getPromptTokens());
        long completionTokens = total == null ? 0L : nz(total.getCompletionTokens());
        long totalTokens = total == null ? 0L : nz(total.getTotalTokens());

        StatOverviewVO vo = new StatOverviewVO();
        vo.setDays(span);
        vo.setBeginTime(beginDate + " 00:00:00");
        vo.setTotalCalls(totalCalls);
        vo.setSuccessCalls(successCalls);
        vo.setFallbackCalls(fallbackCalls);
        vo.setFailCalls(failCalls);
        vo.setSuccessRate(percent(successCalls + fallbackCalls, totalCalls));
        vo.setAvgCostMs(round2(avgCostMs));
        vo.setPromptTokens(promptTokens);
        vo.setCompletionTokens(completionTokens);
        vo.setTotalTokens(totalTokens);
        if (totalTokens > 0) {
            vo.setEstimatedTokens(totalTokens);
            vo.setTokenEstimated(false);
        } else {
            vo.setEstimatedTokens(totalCalls * TOKEN_PER_CALL_ESTIMATE);
            vo.setTokenEstimated(true);
        }

        List<StatOverviewVO.SceneStatVO> scenes = new ArrayList<>();
        List<StatOverviewVO.SceneCountVO> byScene = new ArrayList<>();
        if (sceneRows != null) {
            for (AiStatRow.Scene row : sceneRows) {
                if (row == null) {
                    continue;
                }
                StatOverviewVO.SceneStatVO item = new StatOverviewVO.SceneStatVO();
                item.setScene(row.getScene());
                item.setSceneName(sceneLabel(row.getScene()));
                item.setCalls(nz(row.getCalls()));
                item.setSuccessCalls(nz(row.getSuccessCalls()));
                item.setFallbackCalls(nz(row.getFallbackCalls()));
                item.setFailCalls(nz(row.getFailCalls()));
                item.setAvgCostMs(round2(row.getAvgCostMs() == null ? 0D : row.getAvgCostMs()));
                item.setTotalTokens(nz(row.getTotalTokens()));
                item.setRatio(percent(nz(row.getCalls()), totalCalls));
                scenes.add(item);

                StatOverviewVO.SceneCountVO count = new StatOverviewVO.SceneCountVO();
                count.setScene(row.getScene());
                count.setCount(nz(row.getCalls()));
                byScene.add(count);
            }
        }
        vo.setScenes(scenes);
        vo.setByScene(byScene);

        List<StatOverviewVO.ProviderStatVO> providers = new ArrayList<>();
        if (providerRows != null) {
            for (AiStatRow.Provider row : providerRows) {
                if (row == null) {
                    continue;
                }
                StatOverviewVO.ProviderStatVO item = new StatOverviewVO.ProviderStatVO();
                item.setProvider(row.getProvider());
                item.setCalls(nz(row.getCalls()));
                item.setRatio(percent(nz(row.getCalls()), totalCalls));
                providers.add(item);
            }
        }
        vo.setProviders(providers);

        List<StatOverviewVO.DayCountVO> byDay = new ArrayList<>();
        if (dayRows != null) {
            for (AiStatRow.Day row : dayRows) {
                if (row == null) {
                    continue;
                }
                StatOverviewVO.DayCountVO item = new StatOverviewVO.DayCountVO();
                item.setDate(row.getStatDate());
                item.setCount(nz(row.getCalls()));
                byDay.add(item);
            }
        }
        vo.setByDay(byDay);
        return vo;
    }

    static String sceneLabel(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "未知场景";
        }
        return switch (scene) {
            case "copywrite", "item_copywriting", "copywriting" -> "一键发布文案";
            case "chat", "customer_service" -> "智能客服问答";
            case "recommend" -> "个性化推荐";
            case "audit", "moderation" -> "内容审核";
            case "summary", "item_summary" -> "商品卖点摘要";
            case "price", "item_price" -> "智能估价";
            default -> scene;
        };
    }

    private static double percent(long part, long total) {
        if (total <= 0) {
            return 0D;
        }
        return round2(part * 100D / total);
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
