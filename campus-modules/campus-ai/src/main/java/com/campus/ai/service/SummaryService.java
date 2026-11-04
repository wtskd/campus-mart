package com.campus.ai.service;

import com.campus.ai.domain.dto.SummaryResponse;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.LlmJsonParser;
import com.campus.ai.support.MockContentFactory;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 辅助场景：商品卖点摘要
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ItemDataService itemDataService;

    private final PromptTemplateService promptTemplateService;

    private final AiProviderRouter router;

    private final AiCallLogService callLogService;

    public SummaryResponse summarize(Long itemId) {
        ItemBriefDTO item = itemDataService.getItem(itemId);
        if (item == null) {
            throw new ServiceException("商品不存在或已下架");
        }
        long start = System.currentTimeMillis();
        Map<String, String> vars = buildVariables(item);
        PromptTemplateService.ResolvedPrompt resolved = promptTemplateService.resolve("summary", vars);
        String requestSummary = "itemId=" + itemId + " 标题="
                + MockContentFactory.abbreviate(nvl(item.getTitle()), 80);

        AiResponse response = router.chat("summary", resolved.templateCode(), requestSummary,
                resolved.systemPrompt(), resolved.userPrompt(),
                resolved.temperature(), resolved.maxTokens());

        LlmJsonParser.SummaryResult parsed = null;
        if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
            parsed = LlmJsonParser.parseSummary(response.getContent());
        }
        String provider = response == null ? "mock" : response.getProvider();
        long costMs = response == null ? System.currentTimeMillis() - start : response.getCostMs();
        if (parsed == null || !StringUtils.hasText(parsed.summary())) {
            String mockJson = MockContentFactory.summaryJson(vars);
            parsed = LlmJsonParser.parseSummary(mockJson);
            provider = "mock";
            costMs = System.currentTimeMillis() - start;
            AiResponse fallback = AiResponse.ok("mock", "local-template", mockJson, 0, 0, costMs);
            callLogService.write("summary", resolved.templateCode(), requestSummary, fallback,
                    AiProviderRouter.STATUS_FALLBACK,
                    response == null ? "LLM 无响应" : "LLM 摘要无法解析，已使用本地模板");
        }

        List<String> points = parsed.points() == null ? new ArrayList<>() : parsed.points();
        if (points.isEmpty()) {
            points = defaultPoints(item);
        }
        SummaryResponse result = new SummaryResponse();
        result.setItemId(item.getItemId());
        result.setTitle(item.getTitle());
        result.setSummary(MockContentFactory.abbreviate(parsed.summary(), 120));
        result.setPoints(points);
        result.setProvider(provider);
        result.setCostMs(costMs);
        return result;
    }

    private Map<String, String> buildVariables(ItemBriefDTO item) {
        String title = nvl(item.getTitle());
        String category = nvl(item.getCategoryName());
        String condition = item.getItemCondition() == null ? "" : String.valueOf(item.getItemCondition());
        String price = item.getPrice() == null ? "" : item.getPrice().stripTrailingZeros().toPlainString();
        String description = MockContentFactory.abbreviate(nvl(item.getDescription()), 600);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", title);
        vars.put("标题", title);
        vars.put("category", category);
        vars.put("分类", category);
        vars.put("condition", condition);
        vars.put("成色", condition);
        vars.put("price", price);
        vars.put("价格", price);
        vars.put("description", description);
        vars.put("描述", description);
        vars.put("tags", nvl(item.getTags()));
        vars.put("标签", nvl(item.getTags()));
        return vars;
    }

    private List<String> defaultPoints(ItemBriefDTO item) {
        List<String> points = new ArrayList<>();
        if (StringUtils.hasText(item.getTitle())) {
            points.add(MockContentFactory.abbreviate(item.getTitle().trim(), 24));
        }
        if (item.getPrice() != null) {
            points.add("到手价 ¥" + item.getPrice().stripTrailingZeros().toPlainString());
        }
        if (item.getItemCondition() != null) {
            points.add(conditionLabel(item.getItemCondition()));
        }
        if (points.size() > 3) {
            return new ArrayList<>(points.subList(0, 3));
        }
        return points;
    }

    private String conditionLabel(Integer condition) {
        if (condition == null) {
            return "成色见详情";
        }
        if (condition >= 9) {
            return "成色接近全新，可直接使用";
        }
        if (condition >= 7) {
            return "成色良好，日常使用痕迹轻微";
        }
        return "正常使用痕迹，功能完好";
    }

    private static String nvl(String text) {
        return text == null ? "" : text.trim();
    }
}
