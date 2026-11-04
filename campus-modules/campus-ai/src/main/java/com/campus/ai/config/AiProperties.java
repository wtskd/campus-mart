package com.campus.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置（对应 application.yml 的 campus.ai.*）
 *
 * <pre>
 * campus.ai.provider      remote | local | mock   （默认 remote）
 * campus.ai.remote.*      远程 OpenAI 兼容端点（api-key 为空 -> remote 不可用）
 * campus.ai.local.*       本地 Ollama（OpenAI 兼容端点）
 * campus.ai.timeout       单次调用总超时（秒），超时自动降级
 * campus.ai.moderation.llm-enabled  规则通过后是否再调大模型复核（yml 未配置时默认 true）
 * </pre>
 *
 * @author campus-mart
 */
@Data
@Component
@ConfigurationProperties(prefix = "campus.ai")
public class AiProperties {

    /** 主 provider：remote / local / mock */
    private String provider = "remote";

    /** 单次调用总超时（秒），作为降级兜底的时间上限 */
    private int timeout = 15;

    private Remote remote = new Remote();

    private Local local = new Local();

    private Moderation moderation = new Moderation();

    /** 归一化后的 provider 名称，非法值按 remote 处理 */
    public String providerName() {
        String p = provider == null ? "" : provider.trim().toLowerCase();
        return switch (p) {
            case "local", "remote", "mock" -> p;
            default -> "remote";
        };
    }

    /**
     * 单个 provider 的等待上限（秒）。
     *
     * <p>provider 自身配置只能"收紧"全局上限：取 min(全局, provider)。这样可以保证
     * 前端最多等 campus.ai.timeout 秒，不会因为某个 provider 的长时间超时把页面卡死。</p>
     */
    public int effectiveTimeout(int providerTimeoutSeconds) {
        int global = timeout > 0 ? timeout : 15;
        int own = providerTimeoutSeconds > 0 ? providerTimeoutSeconds : global;
        return Math.max(1, Math.min(global, own));
    }

    @Data
    public static class Remote {
        private String baseUrl = "https://api.deepseek.com/v1";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private int timeout = 30;
    }

    @Data
    public static class Local {
        private String baseUrl = "http://localhost:11434/v1";
        private String model = "qwen2.5:7b-instruct";
        private int timeout = 60;
    }

    @Data
    public static class Moderation {
        /** 规则通过后是否再调大模型复核 */
        private boolean llmEnabled = true;
    }
}
