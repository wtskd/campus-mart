package com.campus.common.log;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 操作日志落库监听器
 *
 * <p>设计说明：所有微服务共用同一个 MySQL 库，与其为「日志」再增加一次 OpenFeign 跨服务调用（多一次网络往返、
 * 多一个故障点），不如各服务把 {@link OperLogEvent} 异步写入同一张 {@code sys_oper_log} 表。
 * 运维侧统一在 campus-system 的日志页面查询即可，这也是单体拆微服务后「共享库 + 异步写日志」的常见折中。</p>
 *
 * <p>写库使用独立的小线程池（队列满则丢弃并打警告），保证日志永远不会拖慢业务请求；
 * 没有数据源的服务（如 campus-auth、campus-gateway）拿到的是空 provider，自动跳过。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
public class OperLogEventListener {

    private static final String INSERT_SQL = """
            INSERT INTO sys_oper_log
              (title, business_type, method, request_method, operator_type, oper_name, dept_name,
               oper_url, oper_ip, oper_param, json_result, status, error_msg, cost_time, oper_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    /** 守护线程池：核心 1、最大 1、队列 2000，满了直接丢（日志不能影响主流程） */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            new NamedThreadFactory("oper-log-writer"),
            new ThreadPoolExecutor.DiscardPolicy());

    public OperLogEventListener(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @EventListener
    public void onOperLog(OperLogEvent event) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return;
        }
        if (executor.isShutdown()) {
            return;
        }
        executor.execute(() -> {
            try {
                jdbcTemplate.update(INSERT_SQL,
                        event.getTitle(),
                        event.getBusinessType(),
                        trim(event.getMethod(), 200),
                        event.getRequestMethod(),
                        event.getOperatorType(),
                        event.getOperName(),
                        event.getDeptName(),
                        trim(event.getOperUrl(), 255),
                        trim(event.getOperIp(), 64),
                        event.getOperParam(),
                        event.getJsonResult(),
                        event.getStatus(),
                        event.getErrorMsg(),
                        event.getCostTime(),
                        event.getOperTime());
            } catch (Exception e) {
                // 日志表写入失败不影响业务，仅打印
                log.warn("写入操作日志失败: {}", e.getMessage());
            }
        });
    }

    private String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 简单命名线程工厂，便于线程 dump 时辨认 */
    private static class NamedThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String prefix;
        private final AtomicInteger seq = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
