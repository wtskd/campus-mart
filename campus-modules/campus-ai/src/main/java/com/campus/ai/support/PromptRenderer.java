package com.campus.ai.support;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板变量渲染
 *
 * <p>同时支持 `{{变量}}`（库内种子模板写法）与 `${变量}`（任务书约定写法）。</p>
 *
 * @author campus-mart
 */
public final class PromptRenderer {

    private static final Pattern VARIABLE = Pattern.compile(
            "\\{\\{\\s*([\\w\\u4e00-\\u9fa5]+)\\s*\\}\\}|\\$\\{\\s*([\\w\\u4e00-\\u9fa5]+)\\s*\\}");

    private static final Pattern LEFT_OVER = Pattern.compile(
            "\\{\\{\\s*[\\w\\u4e00-\\u9fa5]+\\s*\\}\\}|\\$\\{\\s*[\\w\\u4e00-\\u9fa5]+\\s*\\}");

    private PromptRenderer() {
    }

    /**
     * 渲染模板：缺失变量替换为空串，未替换的占位符最后统一清理
     */
    public static String render(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return LEFT_OVER.matcher(template).replaceAll("");
        }
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String value = variables.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(buffer);
        return LEFT_OVER.matcher(buffer.toString()).replaceAll("");
    }
}
