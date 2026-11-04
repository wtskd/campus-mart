package com.campus.ai.provider;

import com.campus.ai.support.MockContentFactory;
import com.campus.ai.support.PromptFields;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 离线兜底 Provider：永远可用，不发起任何网络请求
 *
 * <p>按 prompt 内容识别场景，交给 {@link MockContentFactory} 用中文规则生成：
 * 商品文案（分类句式 + 关键词/成色/价格）、客服问答（组织 RAG 召回的知识条目）、
 * 审核（规则已通过时的合规结论）、摘要/估价。生成的是可直接展示的中文内容，
 * 不会返回"这是模拟回复"之类的占位文案。</p>
 *
 * @author campus-mart
 */
@Component
public class MockProvider implements AiProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public AiResponse chat(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        long start = System.currentTimeMillis();
        String system = systemPrompt == null ? "" : systemPrompt;
        String user = userPrompt == null ? "" : userPrompt;
        String content = generate(system, user);
        long cost = Math.max(1, System.currentTimeMillis() - start);
        return AiResponse.ok(name(), "local-template", content, estimate(system + user), estimate(content), cost);
    }

    private String generate(String system, String user) {
        String marker = system + "\n" + user;
        if (marker.contains("商品关键词") || marker.contains("文案助手") || marker.contains("文案生成")) {
            return MockContentFactory.copywritingJson(PromptFields.parse(user));
        }
        if (marker.contains("【知识库】") || marker.contains("【知识库资料】") || marker.contains("智能客服")) {
            return MockContentFactory.chatAnswer(user);
        }
        if (marker.contains("卖点") || marker.contains("摘要")) {
            return MockContentFactory.summaryJson(PromptFields.parse(user));
        }
        if (marker.contains("审核") || marker.contains("违规")) {
            return MockContentFactory.moderationJson();
        }
        if (marker.contains("估价")) {
            return MockContentFactory.priceJson(PromptFields.parse(user));
        }
        return MockContentFactory.genericAnswer(user);
    }

    /** 粗略 token 估算：中文按 2 字符 ≈ 1 token */
    private static int estimate(String text) {
        return text == null ? 0 : Math.max(1, (int) Math.ceil(text.length() / 2.0));
    }

    /** 供服务层在"LLM 不可用但需要结构化结果"时直接调用（不上报日志，避免重复） */
    public static Map<String, String> fields(String userPrompt) {
        return PromptFields.parse(userPrompt);
    }
}
