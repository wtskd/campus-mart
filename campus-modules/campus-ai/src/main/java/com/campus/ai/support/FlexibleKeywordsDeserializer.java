package com.campus.ai.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 关键词反序列化：兼容前端传字符串（空格/逗号/顿号分隔）或 JSON 数组两种写法。
 *
 * <p>小程序端 {@code publish.vue} 传的是 {@code keywords: "标题 卖点"} 这样的字符串，
 * 而接口文档里是数组，这里做双向兼容，避免 400 让 AI 按钮失效。</p>
 *
 * @author campus-mart
 */
public class FlexibleKeywordsDeserializer extends JsonDeserializer<List<String>> {

    private static final String SPLITTER = "[,，、;；/|\\n\\r\\t]+";

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        List<String> keywords = new ArrayList<>();
        if (node == null || node.isNull()) {
            return keywords;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                add(keywords, child == null ? null : child.asText());
            }
            return keywords;
        }
        // 字符串：按常见分隔符切开；只按空白分隔的整句保留为一条关键词，
        // 以便 LLM 仍能看到"iPhone 12 128G 九成新"这样的完整短语。
        String text = node.asText("");
        for (String part : text.split(SPLITTER)) {
            add(keywords, part);
        }
        return keywords;
    }

    private void add(List<String> keywords, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || keywords.contains(trimmed)) {
            return;
        }
        if (keywords.size() >= 10) {
            return;
        }
        keywords.add(trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed);
    }
}
