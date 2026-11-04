package com.campus.ai.provider;

import com.campus.ai.support.JsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议实现基类（remote / local 共用）
 *
 * <p>请求 POST {base-url}/chat/completions，body 为 messages / temperature / max_tokens，
 * 从响应 usage 取 token 数。HttpClient 连接超时 3 秒、请求超时取 provider 自身配置
 * （由 {@link AiProviderRouter} 再叠加全局兜底超时）。</p>
 *
 * @author campus-mart
 */
@Slf4j
public abstract class AbstractOpenAiProvider implements AiProvider {

    protected final ObjectMapper json = JsonSupport.mapper();

    private final HttpClient httpClient;

    protected AbstractOpenAiProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /** 模型名（健康检查与日志用） */
    public abstract String modelName();

    protected abstract String baseUrl();

    protected abstract String apiKey();

    /** provider 自身配置的超时（秒） */
    protected abstract int timeoutSeconds();

    /** 补全后的 chat/completions 地址 */
    protected String endpoint() {
        String url = baseUrl() == null ? "" : baseUrl().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + "/chat/completions";
    }

    protected Duration requestTimeout() {
        int seconds = timeoutSeconds() > 0 ? timeoutSeconds() : 30;
        return Duration.ofSeconds(Math.max(1, seconds));
    }

    @Override
    public boolean available() {
        return StringUtils.hasText(baseUrl());
    }

    @Override
    public AiResponse chat(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        long start = System.currentTimeMillis();
        String provider = name();
        if (!available()) {
            return AiResponse.fail(provider, modelName(), "provider 不可用（缺少配置）", System.currentTimeMillis() - start);
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelName());

            List<Map<String, String>> messages = new ArrayList<>(2);
            if (StringUtils.hasText(systemPrompt)) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt));
            body.put("messages", messages);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("stream", false);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(requestTimeout())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8));
            if (StringUtils.hasText(apiKey())) {
                builder.header("Authorization", "Bearer " + apiKey().trim());
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long cost = System.currentTimeMillis() - start;

            if (response.statusCode() != 200) {
                return AiResponse.fail(provider, modelName(),
                        "HTTP " + response.statusCode() + " " + truncate(response.body(), 200), cost);
            }
            JsonNode root = json.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return AiResponse.fail(provider, modelName(),
                        "响应缺少 choices：" + truncate(response.body(), 150), cost);
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (!StringUtils.hasText(content)) {
                return AiResponse.fail(provider, modelName(), "模型返回内容为空", cost);
            }
            JsonNode usage = root.path("usage");
            return AiResponse.ok(provider, modelName(), content.trim(),
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0),
                    cost);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AiResponse.fail(provider, modelName(), "调用被中断", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return AiResponse.fail(provider, modelName(),
                    e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 200),
                    System.currentTimeMillis() - start);
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "...";
    }
}
