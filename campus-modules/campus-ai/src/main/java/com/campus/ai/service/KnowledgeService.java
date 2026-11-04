package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.ai.domain.dto.KnowledgeHitVO;
import com.campus.ai.domain.entity.AiKnowledge;
import com.campus.ai.mapper.AiKnowledgeMapper;
import com.campus.ai.mapper.AiKnowledgeSearchMapper;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识库服务：ngram 全文召回 + 管理端 CRUD
 *
 * <p>召回策略：先用 ai_knowledge 的 ft_knowledge(title,content) ngram 全文索引做
 * MATCH ... AGAINST（NATURAL LANGUAGE MODE）取分数；命中不足时用中文二分词做
 * LIKE 兜底（title/keywords/content 命中分别给 0.60/0.45/0.30 分），最后按分数排序去重。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    /** 中文停用词（二分词粒度），避免 LIKE 兜底召回过宽 */
    private static final Set<String> STOP_BIGRAMS = Set.of(
            "如何", "怎么", "可以", "什么", "请问", "一下", "这个", "那个", "是否", "哪些",
            "我想", "我的", "你好", "为什", "么样", "怎样", "能不", "不能", "有没", "没有");

    private final AiKnowledgeSearchMapper searchMapper;

    private final AiKnowledgeMapper knowledgeMapper;

    /**
     * 知识库召回
     *
     * @param question 用户问题
     * @param limit    返回条数（1-10）
     */
    public List<KnowledgeHitVO> recall(String question, int limit) {
        int max = Math.max(1, Math.min(limit, 10));
        List<KnowledgeHitVO> result = new ArrayList<>();
        if (!StringUtils.hasText(question)) {
            return result;
        }
        Map<Long, KnowledgeHitVO> merged = new LinkedHashMap<>();
        String fulltextQuery = cleanFulltextQuery(question);
        if (StringUtils.hasText(fulltextQuery)) {
            try {
                List<KnowledgeHitVO> hits = searchMapper.fulltextSearch(fulltextQuery, max);
                if (hits != null) {
                    for (KnowledgeHitVO hit : hits) {
                        if (hit == null || hit.getKnowledgeId() == null) {
                            continue;
                        }
                        if (hit.getScore() == null || hit.getScore() < 0.05) {
                            continue;
                        }
                        merged.putIfAbsent(hit.getKnowledgeId(), hit);
                    }
                }
            } catch (Exception e) {
                log.warn("知识库全文检索失败，回退关键词匹配: {}", e.getMessage());
            }
        }
        if (merged.size() < max) {
            for (String keyword : fallbackKeywords(question)) {
                if (merged.size() >= max) {
                    break;
                }
                try {
                    List<KnowledgeHitVO> hits = searchMapper.likeSearch(keyword, max);
                    if (hits == null) {
                        continue;
                    }
                    for (KnowledgeHitVO hit : hits) {
                        if (hit == null || hit.getKnowledgeId() == null
                                || merged.containsKey(hit.getKnowledgeId())) {
                            continue;
                        }
                        double score = hit.getScore() == null ? 0.3 : hit.getScore();
                        hit.setScore(Math.min(0.95, score + 0.05));
                        merged.put(hit.getKnowledgeId(), hit);
                    }
                } catch (Exception e) {
                    log.debug("知识库关键词兜底失败: {}", e.getMessage());
                }
            }
        }
        result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparingDouble(
                (KnowledgeHitVO hit) -> hit.getScore() == null ? 0D : hit.getScore()).reversed());
        return result.size() > max ? new ArrayList<>(result.subList(0, max)) : result;
    }

    /** 管理端召回测试 */
    public List<KnowledgeHitVO> search(String question, Integer limit) {
        return recall(question, limit == null ? 5 : limit);
    }

    /* ==================== 管理端 CRUD ==================== */

    public PageResult<AiKnowledge> page(PageQuery query, String kbType, String keyword, String status) {
        LambdaQueryWrapper<AiKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(kbType), AiKnowledge::getKbType, kbType)
                .eq(StringUtils.hasText(status), AiKnowledge::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiKnowledge::getTitle, keyword)
                    .or().like(AiKnowledge::getKeywords, keyword)
                    .or().like(AiKnowledge::getContent, keyword));
        }
        wrapper.orderByDesc(AiKnowledge::getUpdateTime).orderByDesc(AiKnowledge::getId);
        Page<AiKnowledge> page = knowledgeMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    public AiKnowledge create(AiKnowledge knowledge) {
        validate(knowledge);
        knowledge.setId(null);
        if (!StringUtils.hasText(knowledge.getKbType())) {
            knowledge.setKbType("faq");
        }
        if (!StringUtils.hasText(knowledge.getStatus())) {
            knowledge.setStatus("0");
        }
        knowledgeMapper.insert(knowledge);
        return knowledge;
    }

    public AiKnowledge update(AiKnowledge knowledge) {
        ServiceException.throwIf(knowledge == null || knowledge.getId() == null, "知识条目ID不能为空");
        AiKnowledge existing = knowledgeMapper.selectById(knowledge.getId());
        ServiceException.throwIf(existing == null, "知识条目不存在");
        validate(knowledge);
        knowledge.setKbType(StringUtils.hasText(knowledge.getKbType())
                ? knowledge.getKbType() : existing.getKbType());
        knowledge.setStatus(StringUtils.hasText(knowledge.getStatus())
                ? knowledge.getStatus() : existing.getStatus());
        knowledgeMapper.updateById(knowledge);
        return knowledgeMapper.selectById(knowledge.getId());
    }

    public void delete(Long id) {
        ServiceException.throwIf(id == null, "知识条目ID不能为空");
        int rows = knowledgeMapper.deleteById(id);
        ServiceException.throwIf(rows <= 0, "知识条目不存在或已删除");
    }

    private void validate(AiKnowledge knowledge) {
        ServiceException.throwIf(knowledge == null, "参数不能为空");
        ServiceException.throwIf(!StringUtils.hasText(knowledge.getTitle()), "知识标题不能为空");
        ServiceException.throwIf(!StringUtils.hasText(knowledge.getContent()), "知识内容不能为空");
        ServiceException.throwIf(knowledge.getTitle() != null && knowledge.getTitle().length() > 200,
                "知识标题最多 200 字");
    }

    /* ==================== 工具方法 ==================== */

    /** 清理全文检索关键词：去掉特殊字符，过短的查询直接放弃 MATCH */
    private String cleanFulltextQuery(String question) {
        String cleaned = question.replaceAll("[+\\-<>()~*\"@\\\\]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() < 2) {
            return "";
        }
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }

    /** 中文二分词兜底（2 字滑窗 + 去停用词） */
    private List<String> fallbackKeywords(String question) {
        String cleaned = question.replaceAll(
                "[\\s，。！？、；：（）【】「」《》·,.!?;:()\\[\\]{}<>\"'“”‘’]+", "");
        List<String> keywords = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i + 2 <= cleaned.length(); i++) {
            String bigram = cleaned.substring(i, i + 2);
            if (STOP_BIGRAMS.contains(bigram) || !seen.add(bigram)) {
                continue;
            }
            keywords.add(bigram);
            if (keywords.size() >= 6) {
                break;
            }
        }
        if (keywords.isEmpty() && cleaned.length() >= 2) {
            keywords.add(cleaned.substring(0, Math.min(4, cleaned.length())));
        }
        return keywords;
    }
}
