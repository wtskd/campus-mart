package com.campus.ai.provider;

import com.campus.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 远程大模型 Provider（OpenAI 兼容协议，默认 DeepSeek）
 *
 * <p>api-key 为空时视为不可用，启动时 WARN 一次（不重复打印），请求链路自动降级。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Component
public class RemoteOpenAiProvider extends AbstractOpenAiProvider {

    private final AiProperties properties;

    public RemoteOpenAiProvider(AiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void checkConfig() {
        if (!StringUtils.hasText(properties.getRemote().getApiKey())) {
            log.warn("未配置 campus.ai.remote.api-key，远程大模型不可用，AI 调用将自动降级到 local / mock（本提示仅输出一次）");
        } else {
            log.info("远程大模型已配置：{} model={}", properties.getRemote().getBaseUrl(), properties.getRemote().getModel());
        }
    }

    @Override
    public String name() {
        return "remote";
    }

    @Override
    public String modelName() {
        return properties.getRemote().getModel();
    }

    @Override
    protected String baseUrl() {
        return properties.getRemote().getBaseUrl();
    }

    @Override
    protected String apiKey() {
        return properties.getRemote().getApiKey();
    }

    @Override
    protected int timeoutSeconds() {
        return properties.getRemote().getTimeout();
    }

    @Override
    public boolean available() {
        return StringUtils.hasText(baseUrl()) && StringUtils.hasText(apiKey());
    }
}
