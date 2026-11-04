package com.campus.ai.provider;

/**
 * AI 提供方抽象（评分点：可插拔 + 降级）
 *
 * <p>实现：{@link RemoteOpenAiProvider}（远程大模型）、{@link LocalOllamaProvider}（本地 Ollama）、
 * {@link MockProvider}（离线兜底）。</p>
 *
 * @author campus-mart
 */
public interface AiProvider {

    /** provider 名称：remote / local / mock */
    String name();

    /** 是否可用（远程看 api-key 配置，本地看懒探测结果，mock 永远可用） */
    boolean available();

    /**
     * 对话调用
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param temperature  温度
     * @param maxTokens    最大生成 token
     * @return 统一响应（失败时 success=false，error 说明原因）
     */
    AiResponse chat(String systemPrompt, String userPrompt, double temperature, int maxTokens);
}
