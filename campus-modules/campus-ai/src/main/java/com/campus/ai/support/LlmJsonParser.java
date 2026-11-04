package com.campus.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 大模型返回内容解析器（容错）
 *
 * <p>大模型经常返回 markdown 代码块包裹的 JSON，或者前后带解释文字；
 * 这里先剥离 ``` 围栏，再按首个 { 到最后一个 } 提取 JSON，仍失败则退回按行解析。</p>
 *
 * @author campus-mart
 */
public final class LlmJsonParser {

    private static final ObjectMapper MAPPER = JsonSupport.mapper();

    private LlmJsonParser() {
    }

    /** 文案解析结果 */
    public record CopywritingResult(String title, String summary, String description, List<String> tags,
                                    java.math.BigDecimal suggestPrice) {
    }

    /** 摘要解析结果 */
    public record SummaryResult(String summary, List<String> points) {
    }

    /** 审核解析结果 */
    public record ModerationResult(boolean pass, String reason, int riskLevel) {
    }

    /** 估价解析结果 */
    public record PriceResult(java.math.BigDecimal suggestPrice, java.math.BigDecimal minPrice,
                              java.math.BigDecimal maxPrice, String reason) {
    }

    /* ==================== 文案 ==================== */

    public static CopywritingResult parseCopywriting(String raw) {
        JsonNode node = extractJson(raw);
        if (node != null) {
            String title = text(node, "title", "标题");
            String description = text(node, "description", "desc", "描述", "正文");
            String summary = text(node, "summary", "摘要");
            if (description.isBlank()) {
                description = summary;
            }
            List<String> tags = stringList(node.get("tags"));
            if (tags.isEmpty()) {
                tags = stringList(node.get("标签"));
            }
            if (!title.isBlank() || !description.isBlank()) {
                return new CopywritingResult(clean(title), clean(summary), clean(description), tags,
                        decimal(node, "suggestPrice", "suggest_price", "price", "建议价", "参考价"));
            }
        }
        return parseCopywritingByLines(raw);
    }

    /** 行解析：标题：xxx / 描述：xxx / 标签：a,b */
    private static CopywritingResult parseCopywritingByLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String title = "";
        StringBuilder description = new StringBuilder();
        List<String> tags = new ArrayList<>();
        String field = null;
        for (String rawLine : raw.split("\\r?\\n")) {
            String line = rawLine.trim().replaceAll("^[#>*\\-\\s]+", "");
            if (line.isEmpty()) {
                continue;
            }
            String key = null;
            String value = null;
            int colon = colonIndex(line);
            if (colon > 0 && colon <= 8) {
                key = line.substring(0, colon).trim().replaceAll("[【】\\[\\]*]", "");
                value = line.substring(colon + 1).trim();
            }
            String matched = null;
            if (key != null) {
                String lower = key.toLowerCase(Locale.ROOT);
                if (key.contains("标题") || lower.equals("title")) {
                    matched = "title";
                } else if (key.contains("描述") || key.contains("正文") || lower.equals("description")) {
                    matched = "description";
                } else if (key.contains("标签") || lower.equals("tags")) {
                    matched = "tags";
                } else if (key.contains("摘要") || lower.equals("summary")) {
                    matched = "summary";
                }
            }
            if (matched == null) {
                if (field != null && ("description".equals(field) || "summary".equals(field)) && line.length() > 1) {
                    appendLine(description, line);
                }
                continue;
            }
            field = matched;
            switch (matched) {
                case "title" -> title = clean(stripQuotes(value));
                case "description", "summary" -> appendLine(description, value);
                default -> tags.addAll(splitTags(value));
            }
        }
        String desc = description.toString().trim();
        if (title.isBlank() && desc.isBlank()) {
            return null;
        }
        if (title.isBlank()) {
            title = MockContentFactory.abbreviate(desc, 24);
        }
        return new CopywritingResult(title,
                desc.isBlank() ? "" : MockContentFactory.abbreviate(desc, 60), desc, tags, null);
    }

    /* ==================== 估价 ==================== */

    public static PriceResult parsePrice(String raw) {
        JsonNode node = extractJson(raw);
        if (node == null) {
            return null;
        }
        java.math.BigDecimal suggest = decimal(node, "suggestPrice", "suggest_price", "price", "建议价", "预估价格");
        java.math.BigDecimal min = decimal(node, "minPrice", "min_price", "min", "最低价");
        java.math.BigDecimal max = decimal(node, "maxPrice", "max_price", "max", "最高价");
        String reason = text(node, "reason", "原因", "说明");
        if (suggest == null && min != null && max != null) {
            // 模型只给了区间：取中点保守兜底
            suggest = min.add(max).divide(java.math.BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        }
        if (suggest == null && min == null && max == null) {
            return null;
        }
        return new PriceResult(suggest, min, max, reason);
    }

    /* ==================== 摘要 ==================== */

    public static SummaryResult parseSummary(String raw) {
        JsonNode node = extractJson(raw);
        if (node != null) {
            String summary = text(node, "summary", "摘要");
            List<String> points = stringList(node.get("points"));
            if (points.isEmpty()) {
                points = stringList(node.get("卖点"));
            }
            if (!summary.isBlank() || !points.isEmpty()) {
                if (summary.isBlank() && !points.isEmpty()) {
                    summary = String.join("；", points);
                }
                return new SummaryResult(clean(summary), points);
            }
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = clean(raw.replaceAll("(?m)^[#>*\\-\\s]+", ""));
        return text.isBlank() ? null : new SummaryResult(MockContentFactory.abbreviate(text, 120), new ArrayList<>());
    }

    /* ==================== 审核 ==================== */

    public static ModerationResult parseModeration(String raw) {
        JsonNode node = extractJson(raw);
        if (node == null) {
            return null;
        }
        boolean pass = true;
        JsonNode passNode = node.get("pass");
        if (passNode != null) {
            pass = passNode.isBoolean() ? passNode.asBoolean() : Boolean.parseBoolean(passNode.asText("true"));
        }
        String reason = text(node, "reason", "原因");
        String level = text(node, "riskLevel", "risk_level", "risk", "风险等级");
        int riskLevel = switch (level.toLowerCase(Locale.ROOT)) {
            case "high", "高", "3", "2" -> 2;
            case "medium", "medium-risk", "中", "1" -> 1;
            default -> 0;
        };
        if (!pass && riskLevel == 0) {
            riskLevel = 1;
        }
        return new ModerationResult(pass, reason, riskLevel);
    }

    /* ==================== 基础工具 ==================== */

    /** 剥离 markdown 围栏并提取首个 JSON 对象 */
    public static JsonNode extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int start = text.indexOf('\n', fence);
            if (start > 0) {
                text = text.substring(start + 1);
            }
            int end = text.lastIndexOf("```");
            if (end > 0) {
                text = text.substring(0, end);
            }
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        String candidate = text.substring(start, end + 1);
        JsonNode node = tryRead(candidate);
        if (node == null) {
            node = tryRead(candidate.replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\''));
        }
        return node;
    }

    private static JsonNode tryRead(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isValueNode()) {
                String text = value.asText("").trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static java.math.BigDecimal decimal(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual()) {
                String text = value.asText("").replaceAll("[^0-9.\\-]", "").trim();
                if (!text.isEmpty()) {
                    try {
                        return new java.math.BigDecimal(text);
                    } catch (NumberFormatException ignored) {
                        // 忽略非法数字，继续尝试下一个字段
                    }
                }
            }
        }
        return null;
    }

    private static List<String> stringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || node.isNull()) {
            return list;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item != null && item.isValueNode()) {
                    String value = item.asText("").trim();
                    if (!value.isEmpty() && !list.contains(value)) {
                        list.add(value);
                    }
                }
            }
        } else if (node.isTextual()) {
            list.addAll(splitTags(node.asText("")));
        }
        return list;
    }

    private static List<String> splitTags(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tags;
        }
        for (String part : text.replaceAll("^[\\[\\]【】\\s]+|[\\[\\]【】\\s]+$", "").split("[,，、;；/|]+")) {
            String tag = part.trim().replaceAll("^[\"“”']|[\"“”']$", "");
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag.length() > 12 ? tag.substring(0, 12) : tag);
            }
            if (tags.size() >= 5) {
                break;
            }
        }
        return tags;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return stripQuotes(text.replaceAll("(?m)^[#>*\\-\\s]+", "")).trim();
    }

    private static String stripQuotes(String text) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '“' && last == '”')
                    || (first == '\'' && last == '\'') || (first == '「' && last == '」')) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static int colonIndex(String line) {
        int cn = line.indexOf('：');
        int en = line.indexOf(':');
        if (cn < 0) {
            return en;
        }
        if (en < 0) {
            return cn;
        }
        return Math.min(cn, en);
    }

    private static void appendLine(StringBuilder buffer, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (buffer.length() > 0) {
            buffer.append('\n');
        }
        buffer.append(line.trim());
    }
}
