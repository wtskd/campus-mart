package com.campus.ai.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器（规则审核第一道关）
 *
 * <p>内置约 60 个中文敏感词，覆盖涉黄、赌博、毒品、违禁品、诈骗刷单、代写代考、
 * 站外引流等类别；匹配前做归一化（全角转半角、小写、去空格与分隔符），可以识别
 * "加 微 信" 这类简单绕过。高风险词直接判定违规（riskLevel=2），引流类词判定
 * 可疑（riskLevel=1）。另外识别手机号、微信/QQ 号、站外链接。</p>
 *
 * @author campus-mart
 */
@Component
public class SensitiveWordFilter {

    /** 高风险词 -> 违规类别 */
    private static final Map<String, String> HIGH_WORDS = new LinkedHashMap<>();

    /** 中风险词（站外引流 / 营销） */
    private static final Map<String, String> MEDIUM_WORDS = new LinkedHashMap<>();

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Pattern CONTACT = Pattern.compile(
            "(?i)(微信|weixin|vx|v信|扣扣|qq|q群)\\s*[:：]?\\s*[a-zA-Z0-9_-]{5,20}");

    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.)\\S{4,}");

    private static final Pattern STRIP = Pattern.compile("[\\s\\u3000·•.\\-_*|/\\\\]+");

    static {
        // 涉黄
        high("约炮", "色情低俗");
        high("一夜情", "色情低俗");
        high("裸聊", "色情低俗");
        high("援交", "色情低俗");
        high("包养", "色情低俗");
        high("色情", "色情低俗");
        high("黄片", "色情低俗");
        high("成人视频", "色情低俗");
        high("裸照", "色情低俗");
        high("招嫖", "色情低俗");
        // 赌博
        high("赌博", "赌博");
        high("博彩", "赌博");
        high("六合彩", "赌博");
        high("时时彩", "赌博");
        high("赌球", "赌博");
        high("老虎机", "赌博");
        high("私彩", "赌博");
        high("赌场", "赌博");
        // 毒品
        high("毒品", "毒品");
        high("冰毒", "毒品");
        high("海洛因", "毒品");
        high("大麻", "毒品");
        high("摇头丸", "毒品");
        high("k粉", "毒品");
        high("吸毒", "毒品");
        high("制毒", "毒品");
        // 违禁品 / 假证
        high("管制刀具", "违禁品");
        high("仿真枪", "违禁品");
        high("气枪", "违禁品");
        high("弓弩", "违禁品");
        high("电击器", "违禁品");
        high("迷药", "违禁品");
        high("窃听器", "违禁品");
        high("偷拍设备", "违禁品");
        high("假证", "违禁品");
        high("办证", "违禁品");
        high("发票代开", "违禁品");
        // 诈骗 / 违规服务
        high("刷单", "诈骗与违规服务");
        high("返利", "诈骗与违规服务");
        high("套现", "诈骗与违规服务");
        high("洗钱", "诈骗与违规服务");
        high("代写论文", "代写代考");
        high("论文代写", "代写代考");
        high("代考", "代写代考");
        high("替考", "代写代考");
        high("包过考试", "代写代考");
        high("保过", "代写代考");
        high("学历代办", "诈骗与违规服务");

        // 站外引流 / 营销（中风险）
        medium("加微信", "站外引流");
        medium("微信号", "站外引流");
        medium("加vx", "站外引流");
        medium("加qq", "站外引流");
        medium("微商", "站外引流");
        medium("私聊下单", "站外引流");
        medium("站外交易", "站外引流");
        medium("先付定金", "交易风险");
        medium("扫码加好友", "站外引流");
        medium("刷好评", "虚假营销");
        medium("代点赞", "虚假营销");
        medium("水军", "虚假营销");
        medium("引流", "站外引流");
        medium("加好友私聊", "站外引流");
    }

    private static void high(String word, String category) {
        HIGH_WORDS.put(word, category);
    }

    private static void medium(String word, String category) {
        MEDIUM_WORDS.put(word, category);
    }

    /** 审核命中结果 */
    public record Result(boolean hit, int riskLevel, List<String> words, String reason) {
        public static Result safe() {
            return new Result(false, 0, new ArrayList<>(), "规则校验通过：未发现违禁、诈骗或站外引流内容");
        }
    }

    /**
     * 规则审核
     */
    public Result check(String title, String description) {
        String raw = (title == null ? "" : title) + "\n" + (description == null ? "" : description);
        if (!StringUtils.hasText(raw)) {
            return Result.safe();
        }
        String normalized = normalize(raw);

        Set<String> matchedWords = new LinkedHashSet<>();
        Set<String> categories = new LinkedHashSet<>();
        int riskLevel = 0;

        // 1. 高风险词
        for (Map.Entry<String, String> entry : HIGH_WORDS.entrySet()) {
            if (normalized.contains(normalize(entry.getKey()))) {
                matchedWords.add(entry.getKey());
                categories.add(entry.getValue());
                riskLevel = 2;
            }
        }
        // 2. 站外引流 / 营销词
        if (riskLevel < 2) {
            for (Map.Entry<String, String> entry : MEDIUM_WORDS.entrySet()) {
                if (normalized.contains(normalize(entry.getKey()))) {
                    matchedWords.add(entry.getKey());
                    categories.add(entry.getValue());
                    riskLevel = Math.max(riskLevel, 1);
                }
            }
        }
        // 3. 手机号 / 社交账号 / 站外链接（在原始文本上匹配，避免归一化破坏格式）
        Matcher phoneMatcher = PHONE.matcher(raw);
        if (phoneMatcher.find()) {
            matchedWords.add(mask(phoneMatcher.group()));
            categories.add("站外联系方式");
            riskLevel = Math.max(riskLevel, 1);
        }
        Matcher contactMatcher = CONTACT.matcher(raw);
        if (contactMatcher.find()) {
            matchedWords.add(mask(contactMatcher.group()));
            categories.add("站外联系方式");
            riskLevel = Math.max(riskLevel, 1);
        }
        Matcher linkMatcher = LINK.matcher(raw);
        if (linkMatcher.find()) {
            matchedWords.add(linkMatcher.group());
            categories.add("站外链接");
            riskLevel = Math.max(riskLevel, 1);
        }

        if (riskLevel == 0) {
            return Result.safe();
        }
        List<String> words = new ArrayList<>(matchedWords);
        String categoryText = String.join("、", categories);
        String reason = riskLevel >= 2
                ? "内容涉及" + categoryText + "，属于平台禁止发布的信息，请修改后重新提交"
                : "内容包含" + categoryText + "信息，请删除联系方式或引流内容后重新提交";
        return new Result(true, riskLevel, words, reason);
    }

    /** 归一化：全角转半角 + 小写 + 去空格/常见分隔符 */
    private static String normalize(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == '\u3000') {
                builder.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                builder.append((char) (c - 0xFEE0));
            } else {
                builder.append(c);
            }
        }
        return STRIP.matcher(builder.toString().toLowerCase()).replaceAll("");
    }

    private static String mask(String contact) {
        if (contact == null || contact.length() <= 4) {
            return "***";
        }
        return contact.substring(0, Math.min(3, contact.length())) + "***";
    }
}
