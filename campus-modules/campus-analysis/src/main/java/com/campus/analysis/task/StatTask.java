package com.campus.analysis.task;

import com.campus.analysis.service.StatRefreshService;
import com.campus.analysis.util.AnalysisUtils;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统计定时任务。
 *
 * <p>每天凌晨（默认 02:00，可通过 campus.analysis.stat-cron 配置）重算**昨天**的 4 张
 * stat_* 预聚合表；重算幂等（先 delete 当天数据再 insert），失败只记日志不影响服务。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatTask {

    private final StatRefreshService statRefreshService;

    /**
     * 重算昨天的预聚合数据。
     */
    @Scheduled(cron = "${campus.analysis.stat-cron:0 0 2 * * ?}")
    public void refreshYesterday() {
        LocalDate yesterday = AnalysisUtils.today().minusDays(1);
        try {
            statRefreshService.refreshDay(yesterday);
        } catch (Exception e) {
            log.error("[analysis] 凌晨统计重算失败 date={}：{}", yesterday, e.getMessage(), e);
        }
    }
}
