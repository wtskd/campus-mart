package com.campus.analysis.runner;

import com.campus.analysis.service.StatRefreshService;
import com.campus.analysis.util.AnalysisUtils;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动回填：应用启动后静默回填最近 7 天的 4 张 stat_* 预聚合表。
 *
 * <p>保证演示环境「一启动就有数据」；失败只记警告，不影响服务启动。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatBootRunner implements ApplicationRunner {

    /** 启动回填天数 */
    private static final int BOOT_BACKFILL_DAYS = 7;

    private final StatRefreshService statRefreshService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            LocalDate end = AnalysisUtils.today();
            LocalDate start = end.minusDays(BOOT_BACKFILL_DAYS - 1L);
            statRefreshService.refreshRange(start, end);
            log.info("[analysis] 启动回填完成 {} ~ {}", start, end);
        } catch (Exception e) {
            log.warn("[analysis] 启动回填失败（不影响启动）：{}", e.getMessage());
        }
    }
}
