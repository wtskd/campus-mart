package com.campus.ai.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从已渲染的 user prompt 中解析"标签：值"形式的字段
 *
 * <p>MockProvider 用它实现"本地规则生成"：不依赖网络，直接读 prompt 里的
 * 关键词/分类/成色/价格等字段，生成合理的中文内容。</p>
 *
 * @author campus-mart
 */
public final class PromptFields {

    private static final Pattern FIELD_LINE = Pattern.compile(
            "^\\s*[【\\[]?\\s*([^：:\\n]{1,20}?)\\s*[】\\]]?\\s*[：:]\\s*(.*)$");

    private PromptFields() {
    }

    public static Map<String, String> parse(String text) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return fields;
        }
        for (String rawLine : text.split("\\r?\\n")) {
            Matcher matcher = FIELD_LINE.matcher(rawLine.trim());
            if (!matcher.matches()) {
                continue;
            }
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            if (!key.isEmpty()) {
                fields.putIfAbsent(key, value);
            }
        }
        return fields;
    }
}
