package com.campus.ai.service;

import com.campus.ai.domain.dto.CopywritingRequest;
import com.campus.ai.domain.dto.CopywritingResponse;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.LlmJsonParser;
import com.campus.ai.support.MockContentFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场景一：一键发布文案
 *
 * <p>用 ai_prompt_template 中 scene=copywrite 的模板（缺失用内置默认）拼提示词调 LLM，
 * 解析返回 JSON；LLM 不可用或解析失败时用本地模板生成高质量文案，保证前端始终可用。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopywritingService {

    private final PromptTemplateService promptTemplateService;

    private final AiProviderRouter router;

    private final AiCallLogService callLogService;

    public CopywritingResponse generate(CopywritingRequest request) {
        long start = System.currentTimeMillis();
        Map<String, String> vars = buildVariables(request);
        PromptTemplateService.ResolvedPrompt resolved = promptTemplateService.resolve("copywrite", vars);
        String requestSummary = summary(request);

        AiResponse response = router.chat("copywrite", resolved.templateCode(), requestSummary,
                resolved.systemPrompt(), resolved.userPrompt(),
                resolved.temperature(), resolved.maxTokens());

        LlmJsonParser.CopywritingResult parsed = null;
        if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
            parsed = LlmJsonParser.parseCopywriting(response.getContent());
        }
        String provider = response == null ? "mock" : response.getProvider();
        long costMs = response == null ? System.currentTimeMillis() - start : response.getCostMs();
        if (!usable(parsed)) {
            String mockJson = MockContentFactory.copywritingJson(vars);
            parsed = LlmJsonParser.parseCopywriting(mockJson);
            provider = "mock";
            costMs = System.currentTimeMillis() - start;
            AiResponse fallback = AiResponse.ok("mock", "local-template", mockJson, 0, 0, costMs);
            callLogService.write("copywrite", resolved.templateCode(), requestSummary, fallback,
                    AiProviderRouter.STATUS_FALLBACK,
                    response == null ? "LLM 无响应" : "LLM 返回内容无法解析，已使用本地模板");
        }

        CopywritingResponse result = new CopywritingResponse();
        result.setTitle(parsed.title());
        result.setSummary(parsed.summary());
        result.setDescription(parsed.description());
        result.setTags(parsed.tags() == null ? new ArrayList<>() : parsed.tags());
        result.setSuggestPrice(parsed.suggestPrice() != null ? parsed.suggestPrice() : request.getPrice());
        result.setProvider(provider);
        result.setCostMs(costMs);
        return result;
    }

    private boolean usable(LlmJsonParser.CopywritingResult parsed) {
        return parsed != null
                && StringUtils.hasText(parsed.title())
                && StringUtils.hasText(parsed.description());
    }

    private Map<String, String> buildVariables(CopywritingRequest request) {
        String category = firstNonBlank(request.getCategoryName(), request.getCategory(), "闲置好物");
        String keywords = request.getKeywords() == null ? "" : String.join("、", request.getKeywords());
        if (!StringUtils.hasText(keywords)) {
            keywords = category;
        }
        String condition = nvl(request.getCondition());
        String price = priceText(request.getPrice());
        String originalPrice = priceText(request.getOriginalPrice());
        String originalTitle = firstNonBlank(request.getOriginalTitle(), request.getRemark());

        Map<String, String> vars = new LinkedHashMap<>();
        // 英文占位符（{{categoryName}} 等）
        vars.put("categoryName", category);
        vars.put("category", category);
        vars.put("keywords", keywords);
        vars.put("condition", condition);
        vars.put("price", price);
        vars.put("originalPrice", originalPrice);
        vars.put("originalTitle", originalTitle);
        vars.put("title", originalTitle);
        // 库内 ITEM_COPYWRITE 模板用的是 {{remark}}
        vars.put("remark", originalTitle);
        // 中文占位符（{{分类}} 等），同时供本地模板生成使用
        vars.put("分类", category);
        vars.put("商品关键词", keywords);
        vars.put("成色", condition);
        vars.put("期望价", price);
        vars.put("原价", originalPrice);
        vars.put("补充说明", originalTitle);
        return vars;
    }

    private String summary(CopywritingRequest request) {
        return "分类=" + firstNonBlank(request.getCategoryName(), request.getCategory())
                + " 关键词=" + (request.getKeywords() == null ? "" : String.join("/", request.getKeywords()))
                + " 成色=" + nvl(request.getCondition())
                + " 价格=" + priceText(request.getPrice());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String priceText(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String nvl(String text) {
        return text == null ? "" : text.trim();
    }

    @SuppressWarnings("unused")
    private static List<String> safeTags(List<String> tags) {
        return tags == null ? new ArrayList<>() : tags;
    }
}
