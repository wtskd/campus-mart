package com.campus.ai.provider;

import com.campus.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 本地 Ollama Provider（Ollama 提供 OpenAI 兼容端点 /v1）
 *
 * <p>可用性用「懒探测」：GET {base-url}/models，探测结果缓存 60 秒，避免每次调用都去连本地端口；
 * 连接失败视为不可用，直接降级到 mock。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
public class LocalOllamaProvider extends AbstractOpenAiProvider {

    /** 探测结果缓存时长 */
    private static final long PROBE_CACHE_MS = 60_000L;

    private final AiProperties properties;

    private final HttpClient probeClient;

    private final Object probeLock = new Object();

    private volatile long probeAt = 0L;

    private volatile boolean probeOk = false;

    public LocalOllamaProvider(AiProperties properties) {
        this.properties = properties;
        this.probeClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1200))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public String modelName() {
        return properties.getLocal().getModel();
    }

    @Override
    protected String baseUrl() {
        return properties.getLocal().getBaseUrl();
    }

    @Override
    protected String apiKey() {
        return "";
    }

    @Override
    protected int timeoutSeconds() {
        return properties.getLocal().getTimeout();
    }

    @Override
    public boolean available() {
        if (!StringUtils.hasText(baseUrl())) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (probeAt > 0 && now - probeAt < PROBE_CACHE_MS) {
            return probeOk;
        }
        synchronized (probeLock) {
            long current = System.currentTimeMillis();
            if (probeAt > 0 && current - probeAt < PROBE_CACHE_MS) {
                return probeOk;
            }
            boolean ok;
            try {
                String url = baseUrl().trim();
                while (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/models"))
                        .timeout(Duration.ofMillis(1500))
                        .GET()
                        .build();
                HttpResponse<Void> response = probeClient.send(request, HttpResponse.BodyHandlers.discarding());
                ok = response.statusCode() > 0 && response.statusCode() < 500;
            } catch (Exception e) {
                ok = false;
                log.debug("本地 Ollama 探测失败（60 秒内不再重试）: {}", e.getMessage());
            }
            probeAt = System.currentTimeMillis();
            probeOk = ok;
            if (ok) {
                log.info("本地 Ollama 可用：{} model={}", baseUrl(), modelName());
            }
            return ok;
        }
    }

    /** 健康检查展示用：真实连通性（带 60 秒缓存） */
    public boolean reachable() {
        return available();
    }
}
