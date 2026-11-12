package com.campus.ai.service;

import com.campus.ai.config.AiProperties;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.LlmJsonParser;
import com.campus.ai.support.MockContentFactory;
import com.campus.ai.support.SensitiveWordFilter;
import com.campus.api.dto.ModerationDTO;
import com.campus.api.dto.ModerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景四：内容审核（item 服务通过 /ai/inner/moderate 调用）
 *
 * <p>两级策略：先跑内置中文敏感词规则（涉黄赌毒、违禁品、诈骗、代写代考、站外引流等），
 * 命中高风险词直接 pass=false、riskLevel=2；规则通过后再按开关调用 LLM 复核（scene=audit），
 * LLM 不可用时只信规则结果，保证审核链路永不阻塞发布。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final String RULE_TEMPLATE_CODE = "RULE_ENGINE";

    private final SensitiveWordFilter sensitiveWordFilter;

    private final AiProperties aiProperties;

    private final PromptTemplateService promptTemplateService;

    private final AiProviderRouter router;

    private final AiCallLogService callLogService;

    public ModerationDTO moderate(ModerationRequest request) {
        long start = System.currentTimeMillis();
        String title = request == null ? "" : nvl(request.getTitle());
        String description = request == null ? "" : nvl(request.getDescription());
        // 调用方可能只传一整段 content（后台在线试用、评论审核）：标题/描述都为空时回退到 content，
        // 否则规则与模型都对着空文本判断，会误判为「通过」。
        if (!StringUtils.hasText(title) && !StringUtils.hasText(description) && request != null) {
            description = nvl(request.getContent());
        }
        String itemId = request == null || request.getItemId() == null ? "" : String.valueOf(request.getItemId());
        String requestSummary = "itemId=" + itemId + " 标题=" + MockContentFactory.abbreviate(title, 120);

        // 1) 规则引擎：命中即拦截，不浪费一次 LLM 调用
        SensitiveWordFilter.Result rule = sensitiveWordFilter.check(title, description);
        if (rule.hit()) {
            int riskLevel = rule.riskLevel() <= 0 ? 2 : rule.riskLevel();
            ModerationDTO dto = build(false, riskLevel, rule.words(), rule.reason(), "rule",
                    System.currentTimeMillis() - start);
            writeRuleLog(requestSummary, dto);
            return dto;
        }
        // 2) 规则通过 → 可选 LLM 复核
        if (!aiProperties.getModeration().isLlmEnabled()) {
            ModerationDTO dto = build(true, 0, List.of(),
                    "规则校验通过，未发现违规内容。", "rule", System.currentTimeMillis() - start);
            writeRuleLog(requestSummary, dto);
            return dto;
        }

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", title);
        vars.put("标题", title);
        vars.put("description", description);
        vars.put("描述", description);
        vars.put("itemId", itemId);
        PromptTemplateService.ResolvedPrompt resolved = promptTemplateService.resolve("audit", vars);
        AiResponse response = router.chat("audit", resolved.templateCode(), requestSummary,
                resolved.systemPrompt(), resolved.userPrompt(),
                resolved.temperature(), resolved.maxTokens());

        if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
            LlmJsonParser.ModerationResult parsed = LlmJsonParser.parseModeration(response.getContent());
            int riskLevel = parsed.riskLevel();
            boolean pass = parsed.pass();
            if (!pass && riskLevel <= 0) {
                riskLevel = 1;
            }
            String reason = StringUtils.hasText(parsed.reason()) ? parsed.reason() : rule.reason();
            return build(pass, riskLevel, List.of(), reason, response.getProvider(), response.getCostMs());
        }
        // 3) LLM 全链路不可用：规则通过即放行（规则引擎是最终兜底）
        ModerationDTO dto = build(true, 0, List.of(),
                "规则校验通过；AI 复核暂不可用，已按规则结果放行。", "rule",
                System.currentTimeMillis() - start);
        writeRuleLog(requestSummary, dto);
        return dto;
    }

    private ModerationDTO build(boolean pass, int riskLevel, List<String> words, String reason,
                                String provider, long costTime) {
        ModerationDTO dto = new ModerationDTO();
        dto.setPass(pass);
        dto.setRiskLevel(riskLevel);
        dto.setWords(words == null ? new ArrayList<>() : new ArrayList<>(words));
        dto.setReason(reason);
        dto.setProvider(provider);
        dto.setCostTime(costTime);
        return dto;
    }

    /** 规则引擎结果也写调用日志，方便管理端观察审核链路的 provider 分布 */
    private void writeRuleLog(String requestSummary, ModerationDTO dto) {
        AiResponse response = AiResponse.ok("rule", "sensitive-word-v1",
                dto.getReason(), 0, 0, dto.getCostTime() == null ? 0L : dto.getCostTime());
        callLogService.write("audit", RULE_TEMPLATE_CODE, requestSummary,
                response, AiProviderRouter.STATUS_SUCCESS, null);
    }

    private static String nvl(String text) {
        return text == null ? "" : text.trim();
    }
}
