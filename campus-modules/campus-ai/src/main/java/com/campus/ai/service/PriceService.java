package com.campus.ai.service;

import com.campus.ai.domain.dto.PriceRequest;
import com.campus.ai.domain.dto.PriceResponse;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.LlmJsonParser;
import com.campus.ai.support.MockContentFactory;
import com.campus.api.dto.ItemBriefDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 场景五：智能估价
 *
 * <p>优先走远程/本地大模型，按契约返回 {@code {suggestPrice,minPrice,maxPrice,reason}}；
 * 模型不可用或解析失败时用本地估价模板兜底，保证前端永远拿得到价格区间。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private static final BigDecimal MIN_PRICE = BigDecimal.ONE;

    private final PromptTemplateService promptTemplateService;

    private final AiProviderRouter router;

    private final AiCallLogService callLogService;

    private final ItemDataService itemDataService;

    public PriceResponse estimate(PriceRequest request) {
        long start = System.currentTimeMillis();
        PriceRequest source = fillFromItem(request);
        Map<String, String> vars = buildVariables(source);
        PromptTemplateService.ResolvedPrompt resolved = promptTemplateService.resolve("price", vars);
        String requestSummary = summary(source);

        AiResponse response = router.chat("price", resolved.templateCode(), requestSummary,
                resolved.systemPrompt(), resolved.userPrompt(),
                resolved.temperature(), resolved.maxTokens());

        LlmJsonParser.PriceResult parsed = null;
        if (response != null && response.isSuccess() && StringUtils.hasText(response.getContent())) {
            parsed = LlmJsonParser.parsePrice(response.getContent());
        }
        String provider = response == null ? "mock" : response.getProvider();
        long costMs = response == null ? System.currentTimeMillis() - start : response.getCostMs();
        if (!usable(parsed)) {
            String mockJson = MockContentFactory.priceJson(vars);
            parsed = LlmJsonParser.parsePrice(mockJson);
            provider = "mock";
            costMs = System.currentTimeMillis() - start;
            AiResponse fallback = AiResponse.ok("mock", "local-template", mockJson, 0, 0, costMs);
            callLogService.write("price", resolved.templateCode(), requestSummary, fallback,
                    AiProviderRouter.STATUS_FALLBACK,
                    response == null ? "LLM 无响应" : "LLM 返回内容无法解析，已使用本地估价模板");
        }
        if (parsed == null) {
            parsed = new LlmJsonParser.PriceResult(null, null, null, "暂无估价依据，建议参考同类商品成交价");
        }

        PriceResponse result = new PriceResponse();
        BigDecimal suggest = parsed.suggestPrice();
        BigDecimal min = parsed.minPrice();
        BigDecimal max = parsed.maxPrice();
        if (suggest == null && min != null && max != null) {
            suggest = mid(min, max);
        }
        if (suggest == null) {
            suggest = fallbackSuggest(source);
            min = suggest.multiply(new BigDecimal("0.85"));
            max = suggest.multiply(new BigDecimal("1.15"));
        }
        if (min == null) {
            min = suggest.multiply(new BigDecimal("0.85"));
        }
        if (max == null) {
            max = suggest.multiply(new BigDecimal("1.15"));
        }
        min = scale(normalize(min, MIN_PRICE));
        max = scale(normalize(max, min));
        suggest = scale(normalize(suggest, min));
        if (suggest.compareTo(max) > 0) {
            max = suggest;
        }

        result.setSuggestPrice(suggest);
        result.setMinPrice(min);
        result.setMaxPrice(max);
        result.setReason(StringUtils.hasText(parsed.reason()) ? parsed.reason() : defaultReason(source, suggest));
        result.setProvider(provider);
        result.setCostMs(costMs);
        return result;
    }

    private PriceRequest fillFromItem(PriceRequest request) {
        if (request.getItemId() == null) {
            return request;
        }
        ItemBriefDTO item = itemDataService.getItem(request.getItemId());
        if (item == null) {
            return request;
        }
        if (!StringUtils.hasText(request.getTitle())) {
            request.setTitle(item.getTitle());
        }
        if (!StringUtils.hasText(request.getCategoryName()) && !StringUtils.hasText(request.getCategory())) {
            request.setCategoryName(item.getCategoryName());
        }
        if (!StringUtils.hasText(request.getCondition()) && item.getItemCondition() != null) {
            request.setCondition(item.getItemCondition() + "成新");
        }
        if (request.getPrice() == null) {
            request.setPrice(item.getPrice());
        }
        if (!StringUtils.hasText(request.getRemark()) && !StringUtils.hasText(request.getDescription())) {
            request.setDescription(item.getDescription());
        }
        return request;
    }

    private Map<String, String> buildVariables(PriceRequest request) {
        String category = firstNonBlank(request.getCategoryName(), request.getCategory(), "闲置好物");
        String title = firstNonBlank(request.getTitle());
        String condition = firstNonBlank(request.getCondition());
        String originalPrice = priceText(request.getOriginalPrice());
        String price = priceText(request.getPrice());
        String description = abbreviate(firstNonBlank(request.getRemark(), request.getDescription()), 400);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("title", title);
        vars.put("category", category);
        vars.put("categoryName", category);
        vars.put("condition", condition);
        vars.put("originalPrice", originalPrice);
        vars.put("price", price);
        vars.put("description", description);
        vars.put("分类", category);
        vars.put("标题", title);
        vars.put("成色", condition);
        vars.put("原价", originalPrice);
        vars.put("期望价", price);
        vars.put("描述", description);
        return vars;
    }

    private String summary(PriceRequest request) {
        return "分类=" + firstNonBlank(request.getCategoryName(), request.getCategory())
                + " 成色=" + firstNonBlank(request.getCondition())
                + " 原价=" + priceText(request.getOriginalPrice())
                + " 标题=" + abbreviate(firstNonBlank(request.getTitle()), 40);
    }

    private boolean usable(LlmJsonParser.PriceResult parsed) {
        return parsed != null && (parsed.suggestPrice() != null
                || (parsed.minPrice() != null && parsed.maxPrice() != null));
    }

    private BigDecimal fallbackSuggest(PriceRequest request) {
        if (request.getOriginalPrice() != null && request.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return request.getOriginalPrice().multiply(new BigDecimal("0.45"));
        }
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return request.getPrice();
        }
        return new BigDecimal("100");
    }

    private String defaultReason(PriceRequest request, BigDecimal suggest) {
        String base = "参考成色与同类商品成交情况，建议标价 " + suggest.toPlainString() + " 元左右，可留出小刀空间。";
        if (request.getOriginalPrice() != null) {
            return base + "（原价 " + priceText(request.getOriginalPrice()) + " 元）";
        }
        return base;
    }

    private static BigDecimal mid(BigDecimal min, BigDecimal max) {
        return min.add(max).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalize(BigDecimal value, BigDecimal floor) {
        return value.compareTo(floor) < 0 ? floor : value;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String priceText(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String value = text.trim().replaceAll("\\s+", " ");
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
