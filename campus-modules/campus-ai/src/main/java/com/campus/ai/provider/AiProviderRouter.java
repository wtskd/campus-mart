package com.campus.ai.provider;

import com.campus.ai.config.AiProperties;
import com.campus.ai.service.AiCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider 路由与自动降级
 *
 * <p>按 campus.ai.provider 选定主 provider，失败时按 remote → local → mock 顺序自动降级，
 * mock 永远可用，因此对外接口在任何网络环境下都能返回内容。每次调用（无论成功失败）
 * 都会写一条 ai_call_log：主 provider 成功 status='0'；降级后成功 status='2'；全部失败 status='1'。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderRouter {

    /** 主 provider 调用成功 */
    public static final String STATUS_SUCCESS = "0";

    /** 全部 provider 失败 */
    public static final String STATUS_FAIL = "1";

    /** 降级兜底成功（mock 或后续 provider 兜住） */
    public static final String STATUS_FALLBACK = "2";

    private final AiProperties properties;

    private final RemoteOpenAiProvider remoteProvider;

    private final LocalOllamaProvider localProvider;

    private final MockProvider mockProvider;

    private final AiCallLogService callLogService;

    /** 远程 provider 冷却截止时间（毫秒时间戳）：远程刚失败过就跳过它，直接把响应交给兜底 provider */
    private volatile long remoteCoolUntilMs = 0L;

    /** 远程失败后的冷却时长：60 秒，避免用户每条消息都白等一次超时/503 */
    private static final long REMOTE_COOLDOWN_MS = 60_000L;

    /**
     * 统一 LLM 调用入口（带降级链 + 调用日志）
     *
     * @param scene          业务场景（copywrite/chat/audit/summary/recommend）
     * @param templateCode   提示词模板编码，便于回溯
     * @param requestSummary 请求摘要（脱敏后落库）
     */
    public AiResponse chat(String scene, String templateCode, String requestSummary,
                           String systemPrompt, String userPrompt,
                           double temperature, int maxTokens) {
        long start = System.currentTimeMillis();
        String primary = properties.providerName();
        StringBuilder errors = new StringBuilder();
        for (AiProvider provider : chain(primary)) {
            boolean mock = "mock".equals(provider.name());
            if (!mock && !provider.available()) {
                append(errors, provider.name() + ":未配置或不可用");
                continue;
            }
            boolean remote = "remote".equals(provider.name());
            if (remote && System.currentTimeMillis() < remoteCoolUntilMs) {
                append(errors, "remote:冷却中（上次调用失败，直接走兜底）");
                continue;
            }
            AiResponse response;
            try {
                response = provider.chat(systemPrompt, userPrompt, temperature, maxTokens);
            } catch (Exception e) {
                if (remote) {
                    remoteCoolUntilMs = System.currentTimeMillis() + REMOTE_COOLDOWN_MS;
                }
                log.warn("AI provider[{}] 调用异常，尝试下一个兜底：{}", provider.name(), e.getMessage());
                append(errors, provider.name() + ":" + truncate(e.getMessage(), 120));
                continue;
            }
            if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
                String status = provider.name().equals(primary) ? STATUS_SUCCESS : STATUS_FALLBACK;
                if (remote) {
                    remoteCoolUntilMs = 0L;
                }
                if (STATUS_FALLBACK.equals(status)) {
                    log.info("AI 调用降级成功：{} → {}（耗时 {}ms）",
                            primary, provider.name(), response.getCostMs());
                }
                callLogService.write(scene, templateCode, requestSummary, response, status, null);
                return response;
            }
            if (remote) {
                remoteCoolUntilMs = System.currentTimeMillis() + REMOTE_COOLDOWN_MS;
            }
            String reason = response == null ? "空响应" : response.getError();
            append(errors, provider.name() + ":" + truncate(reason, 120));
            log.warn("AI provider[{}] 调用失败，尝试下一个兜底：{}", provider.name(), reason);
        }
        AiResponse failed = AiResponse.fail(primary, "",
                truncate(errors.length() == 0 ? "没有可用的 AI provider" : errors.toString(), 500),
                System.currentTimeMillis() - start);
        callLogService.write(scene, templateCode, requestSummary, failed, STATUS_FAIL, failed.getError());
        return failed;
    }

    /** 当前配置的主 provider 名称 */
    public String primaryProvider() {
        return properties.providerName();
    }

    /** 全部 provider（健康检查用） */
    public List<AiProvider> allProviders() {
        List<AiProvider> providers = new ArrayList<>(3);
        providers.add(remoteProvider);
        providers.add(localProvider);
        providers.add(mockProvider);
        return providers;
    }

    /** 构造降级链：主 provider 在前，mock 永远最后兜底 */
    private List<AiProvider> chain(String primary) {
        List<AiProvider> chain = new ArrayList<>(3);
        if ("mock".equals(primary)) {
            chain.add(mockProvider);
            return chain;
        }
        if ("local".equals(primary)) {
            chain.add(localProvider);
        } else {
            chain.add(remoteProvider);
            chain.add(localProvider);
        }
        chain.add(mockProvider);
        return chain;
    }

    private static void append(StringBuilder builder, String text) {
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(text);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
