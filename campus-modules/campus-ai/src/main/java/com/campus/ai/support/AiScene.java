package com.campus.ai.support;

import lombok.Getter;

import java.util.List;

/**
 * AI 业务场景枚举（唯一口径：以 ai_prompt_template 库内 scene 为准，兼容历史别名）
 *
 * <p>库内种子模板 scene 取值为 copywrite / chat / audit / price；
 * 推荐与摘要在库内没有模板，使用代码内置默认模板（不写库）。</p>
 *
 * @author campus-mart
 */
@Getter
public enum AiScene {

    /** 一键发布文案 */
    COPYWRITE("copywrite", "一键发布文案", "根据分类、关键词、成色、价格生成标题/描述/标签",
            "ITEM_COPYWRITE", List.of("copywrite", "item_copywriting", "copywriting"), true),

    /** 智能客服（RAG） */
    CHAT("chat", "智能客服", "基于 ai_knowledge 知识库的 RAG 问答，支持会话上下文",
            "CUSTOMER_SERVICE", List.of("chat", "customer_service"), true),

    /** 个性化推荐 */
    RECOMMEND("recommend", "个性化推荐", "ItemCF 协同过滤 + 分类/价格带相似度 + 热度兜底",
            null, List.of("recommend"), true),

    /** 内容审核 */
    AUDIT("audit", "内容审核", "内置敏感词规则 + 可选大模型复核",
            "ITEM_AUDIT", List.of("audit", "moderation", "item_audit"), true),

    /** 商品卖点摘要 */
    SUMMARY("summary", "商品卖点摘要", "提炼商品卖点，内置模板（库内无模板时使用）",
            null, List.of("summary", "item_summary"), true),

    /** 智能估价 */
    PRICE("price", "智能估价", "根据分类/成色/原价给出建议售价区间",
            "ITEM_PRICE", List.of("price", "item_price"), true);

    /** 业务场景标识（对外返回用） */
    private final String scene;

    /** 中文名 */
    private final String displayName;

    /** 场景说明 */
    private final String description;

    /** 默认模板编码 */
    private final String templateCode;

    /** 可接受的 scene 别名（库内值优先） */
    private final List<String> aliases;

    /** 是否已对外提供接口 */
    private final boolean implemented;

    AiScene(String scene, String displayName, String description,
            String templateCode, List<String> aliases, boolean implemented) {
        this.scene = scene;
        this.displayName = displayName;
        this.description = description;
        this.templateCode = templateCode;
        this.aliases = aliases;
        this.implemented = implemented;
    }

    /** 按 scene 或别名匹配，未知值兜底为文案场景（避免前端传错直接 500） */
    public static AiScene of(String key) {
        if (key == null || key.isBlank()) {
            return COPYWRITE;
        }
        String k = key.trim().toLowerCase();
        for (AiScene s : values()) {
            if (s.scene.equals(k) || s.aliases.contains(k)) {
                return s;
            }
        }
        return COPYWRITE;
    }

    /** 库内模板查询用的 scene 集合（第一个为库内规范值） */
    public List<String> dbScenes() {
        return aliases;
    }

    /** 内置默认系统提示词（库内模板缺失时兜底） */
    public String defaultSystemPrompt() {
        return switch (this) {
            case COPYWRITE -> """
                    你是校园二手交易平台「校园易站」的资深文案助手，专门帮同学把闲置物品写得真实又有吸引力。
                    要求：
                    1. 标题不超过30字，包含成色、核心卖点和交易方式，不使用夸张促销语；
                    2. 描述分3段：成色说明、使用体验、交易说明，每段50字以内，不编造成色与功能；
                    3. 标签3-5个，每个2-6字；
                    4. 只返回 JSON，不要任何解释：{"title":"","summary":"","description":"","tags":[""]}""";
            case CHAT -> """
                    你是「校园易站」校园二手交易平台的智能客服小易。
                    回答必须严格依据【知识库】中的资料；资料中没有的内容，直接回答"这个问题我暂时不清楚，建议联系平台管理员或人工客服"，禁止编造规则。
                    语气亲切自然，称呼对方为"同学"，回答不超过150字。""";
            case AUDIT -> """
                    你是校园二手交易平台的内容审核员，判断商品标题与描述是否违规。
                    违规类型：违禁品（管制刀具、仿真枪、烟酒药品等）、色情低俗、诈骗与刷单、代写代考、站外引流（加微信/QQ、二维码、外部链接）、辱骂。
                    只返回 JSON，不要解释：{"pass":true,"reason":"","riskLevel":"low"}，riskLevel 取值 low/medium/high。""";
            case SUMMARY -> """
                    你是校园二手平台的商品运营专员，请从商品信息中提炼卖点摘要。
                    只返回 JSON，不要解释：{"summary":"","points":["",""]}。
                    要求：summary 不超过80字，概括成色、核心配置与交易诚意；points 为3条以内短句，每条不超过20字；不夸大、不编造。""";
            case PRICE -> """
                    你是二手商品估价助手。根据商品分类、成色、原价和描述，给出建议售价区间（单位：元）。
                    只返回 JSON，不要解释：{"suggestPrice":0.00,"minPrice":0.00,"maxPrice":0.00,"reason":""}""";
            case RECOMMEND -> """
                    你是校园二手交易平台的推荐运营助手，负责用一句中文短句解释商品为什么值得推荐。
                    只返回 JSON：{"reason":""}，reason 不超过 20 字。""";
        };
    }

    /** 内置默认用户提示词 */
    public String defaultUserPrompt() {
        return switch (this) {
            case COPYWRITE -> """
                    商品关键词：{{keywords}}
                    分类：{{category}}
                    成色：{{condition}}
                    原价：{{originalPrice}}
                    期望价：{{price}}
                    补充说明：{{remark}}""";
            case CHAT -> """
                    【知识库】
                    {{context}}

                    【用户问题】
                    {{question}}""";
            case AUDIT -> """
                    场景：{{scene}}
                    标题：{{title}}
                    描述：{{description}}""";
            case SUMMARY -> """
                    标题：{{title}}
                    分类：{{category}}
                    成色：{{condition}}
                    价格：{{price}}
                    描述：{{description}}""";
            case PRICE -> """
                    分类：{{category}}
                    成色：{{condition}}
                    原价：{{originalPrice}}
                    描述：{{description}}""";
            case RECOMMEND -> """
                    用户画像：{{profile}}
                    候选商品：{{item}}""";
        };
    }

    /** 内置默认温度 */
    public double defaultTemperature() {
        return switch (this) {
            case COPYWRITE -> 0.80;
            case CHAT -> 0.30;
            case AUDIT -> 0.10;
            case SUMMARY -> 0.50;
            case PRICE -> 0.50;
            case RECOMMEND -> 0.30;
        };
    }

    /** 内置默认 max_tokens */
    public int defaultMaxTokens() {
        return switch (this) {
            case COPYWRITE -> 800;
            case CHAT -> 500;
            case AUDIT -> 300;
            case SUMMARY -> 400;
            case PRICE -> 300;
            case RECOMMEND -> 200;
        };
    }
}
