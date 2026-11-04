package com.campus.ai.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 离线 mock 内容工厂
 *
 * <p>无网络/无 API key 时，基于模板 + 中文规则生成"看起来像真的"内容：
 * 文案按分类套用不同句式（成色说明 / 使用体验 / 交易说明），客服直接组织
 * RAG 召回的知识条目，摘要/估价按字段生成。演示兜底不能用"这是模拟回复"糊弄。</p>
 *
 * @author campus-mart
 */
public final class MockContentFactory {

    /** 分类画像：{外观成色, 使用体验, 适合人群} */
    private static final Map<String, String[]> CATEGORY_PROFILE = new LinkedHashMap<>();

    /** 分类标题后缀 */
    private static final Map<String, String> TITLE_SUFFIX = new LinkedHashMap<>();

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    static {
        CATEGORY_PROFILE.put("数码电子", new String[]{
                "机身与屏幕没有明显磕碰，功能全部正常，电池健康度良好",
                "日常上课、写作业、追剧都在用，系统流畅不卡顿",
                "想换设备或者预算有限的同学"});
        CATEGORY_PROFILE.put("教材书籍", new String[]{
                "书页完整无缺页，只有少量铅笔笔记，不影响阅读",
                "重点章节都标注过，期末复习可以直接上手",
                "本学期开课、想省下教材费的同学"});
        CATEGORY_PROFILE.put("生活用品", new String[]{
                "日常使用痕迹轻微，已清洁整理，可以直接用",
                "宿舍常用款，用着顺手，闲置下来才出",
                "刚开学需要添置日用品的同学"});
        CATEGORY_PROFILE.put("运动户外", new String[]{
                "使用次数不多，没有破损和明显磨损",
                "周末打球、跑步时用过几回，手感和新的一样",
                "喜欢运动、想先入手入门装备的同学"});
        CATEGORY_PROFILE.put("美妆个护", new String[]{
                "正品自用，日期新鲜，包装完整可当面验货",
                "买多了一直闲置，质地和气味都正常",
                "想试试这个牌子又不想买正装的同学"});
        CATEGORY_PROFILE.put("宿舍家具", new String[]{
                "结构稳固没有松动，可拆卸，搬运方便",
                "宿舍里用了一年，桌面和框架都完好",
                "刚搬宿舍、需要添置家具的同学"});
        CATEGORY_PROFILE.put("乐器文娱", new String[]{
                "音准正常，配件齐全，没有暗伤",
                "社团和宿舍都练过，手感舒适",
                "想入门或者想换把更好的同学"});

        TITLE_SUFFIX.put("数码电子", "功能正常 校内面交");
        TITLE_SUFFIX.put("教材书籍", "笔记少 期末可用");
        TITLE_SUFFIX.put("生活用品", "干净整洁 可自提");
        TITLE_SUFFIX.put("运动户外", "用得不勤 无磕碰");
        TITLE_SUFFIX.put("美妆个护", "正品可验 日期新");
        TITLE_SUFFIX.put("宿舍家具", "结实稳固 可搬走");
        TITLE_SUFFIX.put("乐器文娱", "音准正常 配件齐");
    }

    private MockContentFactory() {
    }

    /* ==================== 场景一：商品文案 ==================== */

    public static String copywritingJson(Map<String, String> vars) {
        String keywordText = firstNonBlank(vars.get("商品关键词"), vars.get("keywords"));
        String category = blankToDefault(vars.get("分类"), "闲置好物");
        String condition = conditionTag(vars.get("成色"));
        String priceText = firstNonBlank(vars.get("期望价"), vars.get("price"));
        String originalPriceText = firstNonBlank(vars.get("原价"), vars.get("originalPrice"));
        String remark = firstNonBlank(vars.get("补充说明"), vars.get("remark"));

        List<String> keywords = splitKeywords(keywordText);
        String core = keywords.isEmpty() ? category : keywords.get(0);
        String[] profile = profile(category);

        String suffix = TITLE_SUFFIX.getOrDefault(matchProfileKey(category), "成色好 可小刀");
        String title = "【" + condition + "】" + core + " " + suffix;
        if (title.length() > 30) {
            title = "【" + condition + "】" + core + " 校内面交";
        }
        if (title.length() > 30) {
            title = "【" + condition + "】" + abbreviate(core, 18);
        }

        BigDecimal price = numeric(priceText);
        BigDecimal originalPrice = numeric(originalPriceText);

        String conditionDesc = "全新".equals(condition)
                ? "全新未使用，包装与配件齐全"
                : condition + "，" + profile[0];
        String experience = profile[1];
        if (!remark.isBlank()) {
            experience = experience + "；" + abbreviate(remark, 36);
        }

        StringBuilder trade = new StringBuilder();
        if (price != null) {
            trade.append("到手价 ").append(plain(price)).append(" 元，价格可小刀");
        } else {
            trade.append("价格好商量，欢迎私聊");
        }
        if (originalPrice != null) {
            trade.append("（原价 ").append(plain(originalPrice)).append(" 元）");
        }
        trade.append("；校内当面交易（图书馆、宿舍楼下均可），支持当场验货，不合适可以直接不交易。");

        String description = "【成色说明】" + conditionDesc + "。\n"
                + "【使用体验】" + experience + "，适合" + profile[2] + "。\n"
                + "【交易说明】" + trade;

        String summary = condition + core + "，"
                + (price != null ? plain(price) + "元" : "价格可议") + "，"
                + category + "，校内面交可小刀。";

        List<String> tags = new ArrayList<>();
        addTag(tags, category.length() <= 6 ? category : category.substring(0, 4));
        addTag(tags, condition);
        for (String keyword : keywords) {
            if (tags.size() >= 5) {
                break;
            }
            addTag(tags, keyword.length() > 16 ? keyword.substring(0, 16) : keyword);
        }
        addTag(tags, "校内面交");
        addTag(tags, "可小刀");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", title);
        result.put("summary", summary);
        result.put("description", description);
        result.put("tags", tags);
        if (price != null) {
            result.put("suggestPrice", price);
        }
        return JsonSupport.write(result);
    }

    /* ==================== 场景二：智能客服 ==================== */

    public static String chatAnswer(String userPrompt) {
        String prompt = userPrompt == null ? "" : userPrompt.replace("【知识库资料】", "【知识库】");
        String context = between(prompt, "【知识库】", "【用户问题】");
        String question = after(prompt, "【用户问题】");
        if (question.isBlank()) {
            question = "";
        }
        KnowledgeBlock block = firstBlock(context);
        if (block == null || block.content().isBlank()) {
            return unknownAnswer(question);
        }
        String body = abbreviate(block.content().replaceAll("-{3,}", " ").replaceAll("\\s+", " ").trim(), 120);
        body = stripTrailingPunctuation(body);
        return "同学你好～根据平台《" + block.title() + "》：" + body
                + "。\n如果还有疑问可以继续问我，也可以联系管理员协助处理。";
    }

    private static String unknownAnswer(String question) {
        String topic = question.isBlank() ? "这个问题" : "「" + abbreviate(question, 24) + "」";
        return "同学你好～关于" + topic + "，我暂时没有在平台资料里找到明确答案。"
                + "建议到「帮助中心」查看常见问题，或在「我的-意见反馈」提交，管理员会在 1 个工作日内回复。"
                + "你也可以直接问我：如何发布商品、交易流程、退款与取消规则、信用分怎么算。";
    }

    private record KnowledgeBlock(String title, String content) {
    }

    private static KnowledgeBlock firstBlock(String context) {
        if (context == null || context.isBlank()) {
            return null;
        }
        String first = context.split("\\n-{3,}\\n")[0].trim();
        int newline = first.indexOf('\n');
        if (newline < 0) {
            return null;
        }
        String header = first.substring(0, newline).trim();
        String content = first.substring(newline + 1).trim();
        String title = header;
        if (title.startsWith("[")) {
            int close = title.indexOf(']');
            if (close > 0) {
                title = title.substring(close + 1).trim();
            }
        }
        int bracket = title.indexOf('（');
        if (bracket > 0) {
            title = title.substring(0, bracket).trim();
        }
        return new KnowledgeBlock(title.isBlank() ? "平台规则" : title, content);
    }

    /* ==================== 场景四：内容审核 ==================== */

    public static String moderationJson() {
        return "{\"pass\":true,\"reason\":\"规则校验未发现违禁品、诈骗、色情或站外引流内容，可以正常发布。\",\"riskLevel\":\"low\"}";
    }

    /* ==================== 场景五：商品摘要 ==================== */

    public static String summaryJson(Map<String, String> fields) {
        String title = blankToDefault(firstNonBlank(fields.get("标题"), fields.get("title")), "这件商品");
        String category = blankToDefault(firstNonBlank(fields.get("分类"), fields.get("category")), "闲置好物");
        String condition = conditionTag(firstNonBlank(fields.get("成色"), fields.get("condition")));
        String priceText = firstNonBlank(fields.get("价格"), fields.get("price"));
        String description = firstNonBlank(fields.get("描述"), fields.get("description"));
        String[] profile = profile(category);
        BigDecimal price = numeric(priceText);
        String snippet = description.isBlank() ? profile[1] : abbreviate(description, 46);

        String summary = condition + title + "，" + category + "，"
                + (price != null ? plain(price) + "元" : "价格可议") + "；" + snippet;
        summary = abbreviate(summary, 80);

        List<String> points = new ArrayList<>();
        points.add("成色：" + condition);
        points.add(price != null ? "价格：" + plain(price) + "元，可小刀" : "价格可谈，欢迎私聊");
        points.add("校内面交，可当场验货");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("points", points);
        return JsonSupport.write(result);
    }

    /* ==================== 场景：智能估价（模板已入库） ==================== */

    public static String priceJson(Map<String, String> fields) {
        BigDecimal originalPrice = numeric(firstNonBlank(fields.get("原价"), fields.get("originalPrice")));
        double base = originalPrice != null ? originalPrice.doubleValue() * 0.45 : 120.0;
        long low = Math.max(10, Math.round(base * 0.75 / 10.0) * 10);
        long high = Math.max(low + 20, Math.round(base * 1.15 / 10.0) * 10);
        String reason = "参考原价与成色，二手成交价通常在原价的 35%-55% 之间，建议标价 "
                + ((low + high) / 2) + " 元左右，留出小刀空间。";
        Map<String, Object> result = new LinkedHashMap<>();
        long suggest = Math.max(10, Math.round((low + high) / 2.0 / 10.0) * 10);
        result.put("suggestPrice", suggest);
        result.put("minPrice", low);
        result.put("maxPrice", high);
        result.put("reason", reason);
        return JsonSupport.write(result);
    }

    /* ==================== 通用兜底 ==================== */

    public static String genericAnswer(String userPrompt) {
        String topic = abbreviate((userPrompt == null ? "" : userPrompt).replaceAll("\\s+", " "), 60);
        return "同学你好～关于「" + topic + "」，建议先到「帮助中心」查看对应的规则说明；"
                + "如果仍未解决，可以在「我的-意见反馈」提交，管理员会在 1 个工作日内回复你。";
    }

    /* ==================== 工具方法 ==================== */

    private static String[] profile(String category) {
        String[] matched = CATEGORY_PROFILE.get(matchProfileKey(category));
        return matched != null ? matched : new String[]{
                "外观成色与描述一致，没有功能性问题",
                "平时用得很爱惜，只有正常使用痕迹",
                "需要这类好物的同学"};
    }

    private static String matchProfileKey(String category) {
        if (category == null) {
            return "";
        }
        for (String key : CATEGORY_PROFILE.keySet()) {
            if (category.contains(key) || key.contains(category)) {
                return key;
            }
        }
        if (category.contains("手机") || category.contains("电脑") || category.contains("数码")
                || category.contains("平板") || category.contains("耳机")) {
            return "数码电子";
        }
        if (category.contains("书") || category.contains("教材") || category.contains("资料")) {
            return "教材书籍";
        }
        if (category.contains("运动") || category.contains("球") || category.contains("健身")) {
            return "运动户外";
        }
        if (category.contains("妆") || category.contains("护肤")) {
            return "美妆个护";
        }
        if (category.contains("桌") || category.contains("椅") || category.contains("收纳")) {
            return "宿舍家具";
        }
        if (category.contains("乐器") || category.contains("吉他") || category.contains("琴")) {
            return "乐器文娱";
        }
        return "";
    }

    private static String conditionTag(String condition) {
        if (condition == null || condition.isBlank()) {
            return "九成新";
        }
        String value = condition.replace("成色", "").trim();
        if (value.contains("全新") || value.contains("未拆") || value.contains("未使用")) {
            return "全新";
        }
        if (value.matches("^\\d+$")) {
            return value + "成新";
        }
        value = value.replaceAll("^(\\d+)新$", "$1成新");
        return value.length() > 6 ? value.substring(0, 6) : value;
    }

    /** 中文场景下按长度截断，尽量断在标点处 */
    public static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= max) {
            return clean;
        }
        int floor = Math.max(1, max * 2 / 3);
        for (int i = max - 1; i >= floor; i--) {
            char c = clean.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '，') {
                return clean.substring(0, i + 1);
            }
        }
        return clean.substring(0, max) + "…";
    }

    private static List<String> splitKeywords(String keywords) {
        List<String> list = new ArrayList<>();
        if (keywords == null || keywords.isBlank()) {
            return list;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : keywords.split("[,，、;；/|]+")) {
            String keyword = part.trim();
            if (!keyword.isEmpty() && seen.add(keyword)) {
                list.add(keyword);
            }
            if (list.size() >= 4) {
                break;
            }
        }
        return list;
    }

    private static void addTag(List<String> tags, String tag) {
        if (tag == null) {
            return;
        }
        String value = tag.trim();
        if (value.isEmpty() || tags.contains(value) || tags.size() >= 5) {
            return;
        }
        tags.add(value.length() > 16 ? value.substring(0, 16) : value);
    }

    private static BigDecimal numeric(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(text.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String between(String text, String startMarker, String endMarker) {
        if (text == null) {
            return "";
        }
        int start = text.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        String rest = text.substring(start + startMarker.length());
        int end = rest.indexOf(endMarker);
        return end >= 0 ? rest.substring(0, end).trim() : rest.trim();
    }

    private static String after(String text, String marker) {
        if (text == null) {
            return "";
        }
        int index = text.indexOf(marker);
        return index < 0 ? "" : text.substring(index + marker.length()).trim();
    }

    private static String stripTrailingPunctuation(String text) {
        String result = text;
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last == '。' || last == '；' || last == '，' || last == '！' || last == '？' || last == '…' || last == '.') {
                result = result.substring(0, result.length() - 1).trim();
            } else {
                break;
            }
        }
        return result;
    }
}
