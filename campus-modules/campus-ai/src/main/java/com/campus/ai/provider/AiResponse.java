package com.campus.ai.provider;

import lombok.Data;

/**
 * 大模型统一响应
 *
 * @author campus-mart
 */
@Data
public class AiResponse {

    /** 生成内容 */
    private String content;

    /** 实际提供方 remote / local / mock */
    private String provider;

    /** 实际模型 */
    private String model;

    /** 输入 token */
    private Integer promptTokens;

    /** 输出 token */
    private Integer completionTokens;

    /** 耗时（毫秒） */
    private long costMs;

    /** 是否成功 */
    private boolean success;

    /** 失败原因 */
    private String error;

    public static AiResponse ok(String provider, String model, String content,
                                Integer promptTokens, Integer completionTokens, long costMs) {
        AiResponse r = new AiResponse();
        r.setProvider(provider);
        r.setModel(model);
        r.setContent(content);
        r.setPromptTokens(promptTokens == null ? 0 : promptTokens);
        r.setCompletionTokens(completionTokens == null ? 0 : completionTokens);
        r.setCostMs(costMs);
        r.setSuccess(true);
        return r;
    }

    public static AiResponse fail(String provider, String model, String error, long costMs) {
        AiResponse r = new AiResponse();
        r.setProvider(provider);
        r.setModel(model);
        r.setCostMs(costMs);
        r.setSuccess(false);
        r.setError(error);
        return r;
    }

    /** 总 token（落库用） */
    public int totalTokens() {
        return (promptTokens == null ? 0 : promptTokens) + (completionTokens == null ? 0 : completionTokens);
    }
}
