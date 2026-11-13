package com.campus.ai.service;

import com.campus.ai.domain.dto.ChatRequest;
import com.campus.ai.domain.dto.ChatResponse;
import com.campus.ai.domain.dto.KnowledgeHitVO;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.ChatTurn;
import com.campus.ai.support.MockContentFactory;
import com.campus.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 场景二：智能客服（RAG 知识库问答）
 *
 * <p>流程：ai_knowledge ngram 全文召回 top3 → 拼进 system prompt 约束"只能根据资料回答"
 * → 调 LLM；无召回或 LLM 不可用时用本地模板把最相关知识条目组织成答案。
 * 会话上下文按 sessionId 存 Redis（key=ai:chat:{sessionId}，最近 6 轮，TTL 30 分钟）。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int RECALL_LIMIT = 5;

    private static final int HISTORY_ROUNDS = 6;

    private static final long HISTORY_TTL_SECONDS = 30 * 60L;

    private static final String SESSION_KEY_PREFIX = "ai:chat:";

    private static final String ANSWER_RULE =
            "\n回答要求：只能依据上述【知识库资料】回答；资料中没有的信息必须说明暂时没有相关资料，"
                    + "并建议联系平台管理员或查看帮助中心；不要编造政策、价格和流程。";

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "如何发布商品", "交易流程是怎样的", "退款与取消规则", "如何联系卖家");

    private final KnowledgeService knowledgeService;

    private final PromptTemplateService promptTemplateService;

    private final AiProviderRouter router;

    private final AiCallLogService callLogService;

    private final RedisUtils redisUtils;

    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : UUID.randomUUID().toString().replace("-", "");
        String requestSummary = "question=" + MockContentFactory.abbreviate(question, 200);

        List<KnowledgeHitVO> hits = knowledgeService.recall(question, RECALL_LIMIT);
        String knowledgeBlock = buildKnowledgeBlock(hits);
        List<ChatTurn> history = loadHistory(sessionId);
        String historyBlock = buildHistoryBlock(history);

        Map<String, String> vars = new HashMap<>();
        vars.put("question", question);
        vars.put("问题", question);
        vars.put("knowledge", knowledgeBlock);
        vars.put("知识库", knowledgeBlock);
        // 库内 CUSTOMER_SERVICE 模板用的是 {{context}}
        vars.put("context", knowledgeBlock);
        vars.put("history", historyBlock);
        vars.put("itemId", request.getItemId() == null ? "" : String.valueOf(request.getItemId()));
        PromptTemplateService.ResolvedPrompt resolved = promptTemplateService.resolve("chat", vars);

        String systemPrompt = resolved.systemPrompt().contains("【知识库资料】")
                ? resolved.systemPrompt()
                : resolved.systemPrompt() + "\n\n" + knowledgeBlock + ANSWER_RULE;
        String userPrompt = (historyBlock.isEmpty() ? "" : historyBlock + "\n") + resolved.userPrompt();

        AiResponse response = router.chat("chat", resolved.templateCode(), requestSummary,
                systemPrompt, userPrompt, resolved.temperature(), resolved.maxTokens());

        String answer;
        String provider;
        long costMs;
        if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
            answer = response.getContent().trim();
            provider = response.getProvider();
            costMs = response.getCostMs();
        } else {
            answer = localAnswer(question, knowledgeBlock);
            provider = "mock";
            costMs = System.currentTimeMillis() - start;
            AiResponse fallback = AiResponse.ok("mock", "local-template", answer, 0, 0, costMs);
            callLogService.write("chat", resolved.templateCode(), requestSummary, fallback,
                    AiProviderRouter.STATUS_FALLBACK,
                    response == null ? "LLM 无响应" : response.getError());
        }

        saveHistory(sessionId, question, answer);

        ChatResponse result = new ChatResponse();
        result.setAnswer(answer);
        result.setSessionId(sessionId);
        result.setProvider(provider);
        result.setCostMs(costMs);
        result.setSources(toSources(hits));
        result.setSuggestions(buildSuggestions(hits));
        return result;
    }

    private List<ChatResponse.Source> toSources(List<KnowledgeHitVO> hits) {
        List<ChatResponse.Source> sources = new ArrayList<>();
        for (KnowledgeHitVO hit : hits) {
            if (hit == null) {
                continue;
            }
            sources.add(new ChatResponse.Source(hit.getKnowledgeId(), hit.getTitle(), hit.getScore(),
                    MockContentFactory.abbreviate(hit.getContent(), 260)));
        }
        return sources;
    }

    /**
     * 组装知识库资料块（同时也是 MockContentFactory 解析的标准格式）
     */
    private String buildKnowledgeBlock(List<KnowledgeHitVO> hits) {
        StringBuilder builder = new StringBuilder("【知识库资料】\n");
        if (hits == null || hits.isEmpty()) {
            builder.append("（本次未检索到相关资料）\n");
            return builder.toString();
        }
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeHitVO hit = hits.get(i);
            double score = hit.getScore() == null ? 0D : hit.getScore();
            builder.append('[').append(i + 1).append("] ").append(hit.getTitle())
                    .append("（相关度 ").append(String.format("%.3f", score)).append("）\n")
                    .append(MockContentFactory.abbreviate(hit.getContent(), 300))
                    .append("\n---\n");
        }
        return builder.toString().trim();
    }

    private String buildHistoryBlock(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("【对话历史】\n");
        for (ChatTurn turn : history) {
            builder.append("用户：").append(MockContentFactory.abbreviate(turn.getQuestion(), 100)).append('\n')
                    .append("客服：").append(MockContentFactory.abbreviate(turn.getAnswer(), 160)).append('\n');
        }
        return builder.toString().trim();
    }

    /** LLM 全链路不可用时的本地答案：用最相关知识库条目组织 */
    private String localAnswer(String question, String knowledgeBlock) {
        String prompt = knowledgeBlock + "\n【用户问题】\n" + question;
        String answer = MockContentFactory.chatAnswer(prompt);
        return StringUtils.hasText(answer) ? answer : "同学你好～这个问题我暂时没有查到相关资料，建议联系平台管理员或前往帮助中心咨询。";
    }

    private List<String> buildSuggestions(List<KnowledgeHitVO> hits) {
        List<String> suggestions = new ArrayList<>();
        if (hits != null) {
            for (KnowledgeHitVO hit : hits) {
                if (suggestions.size() >= 2) {
                    break;
                }
                if (hit != null && StringUtils.hasText(hit.getTitle())) {
                    suggestions.add("关于「" + MockContentFactory.abbreviate(hit.getTitle(), 18) + "」");
                }
            }
        }
        for (String item : DEFAULT_SUGGESTIONS) {
            if (suggestions.size() >= 4) {
                break;
            }
            if (!suggestions.contains(item)) {
                suggestions.add(item);
            }
        }
        return suggestions;
    }

    /* ==================== 会话上下文（Redis） ==================== */

    @SuppressWarnings("unchecked")
    private List<ChatTurn> loadHistory(String sessionId) {
        try {
            Object cached = redisUtils.get(SESSION_KEY_PREFIX + sessionId);
            if (cached instanceof List<?> list) {
                List<ChatTurn> turns = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof ChatTurn turn) {
                        turns.add(turn);
                    }
                }
                return turns;
            }
        } catch (Exception e) {
            log.debug("读取 AI 会话上下文失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveHistory(String sessionId, String question, String answer) {
        try {
            List<ChatTurn> history = loadHistory(sessionId);
            history.add(new ChatTurn(MockContentFactory.abbreviate(question, 200),
                    MockContentFactory.abbreviate(answer, 300), System.currentTimeMillis()));
            while (history.size() > HISTORY_ROUNDS) {
                history.remove(0);
            }
            redisUtils.set(SESSION_KEY_PREFIX + sessionId, history, HISTORY_TTL_SECONDS);
        } catch (Exception e) {
            log.debug("保存 AI 会话上下文失败：{}", e.getMessage());
        }
    }
}
