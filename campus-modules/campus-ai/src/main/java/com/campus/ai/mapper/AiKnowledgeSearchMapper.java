package com.campus.ai.mapper;

import com.campus.ai.domain.dto.KnowledgeHitVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库检索 Mapper（ai_knowledge 的 ngram 全文索引）
 *
 * <p>注意：ai_knowledge 的 ft_knowledge(title, content) 使用 ngram 解析器，
 * MySQL 5.7+/8.0 下 `MATCH ... AGAINST(... IN NATURAL LANGUAGE MODE)` 可直接使用。
 * 中文短问句命中率受 ngram_token_size 影响，因此额外提供 LIKE 兜底方法。</p>
 *
 * @author campus-mart
 */
@Mapper
public interface AiKnowledgeSearchMapper {

    @Select("SELECT id AS knowledge_id, kb_type, title, content, keywords, "
            + "MATCH(title, content) AGAINST(#{q} IN NATURAL LANGUAGE MODE) AS score "
            + "FROM ai_knowledge WHERE status = '0' "
            + "AND MATCH(title, content) AGAINST(#{q} IN NATURAL LANGUAGE MODE) "
            + "ORDER BY score DESC LIMIT #{limit}")
    List<KnowledgeHitVO> fulltextSearch(@Param("q") String q, @Param("limit") int limit);

    @Select("SELECT id AS knowledge_id, kb_type, title, content, keywords, "
            + "CASE WHEN title LIKE CONCAT('%', #{kw}, '%') THEN 0.60 "
            + "WHEN keywords LIKE CONCAT('%', #{kw}, '%') THEN 0.45 "
            + "ELSE 0.30 END AS score "
            + "FROM ai_knowledge WHERE status = '0' "
            + "AND (title LIKE CONCAT('%', #{kw}, '%') OR keywords LIKE CONCAT('%', #{kw}, '%') "
            + "OR content LIKE CONCAT('%', #{kw}, '%')) "
            + "ORDER BY score DESC LIMIT #{limit}")
    List<KnowledgeHitVO> likeSearch(@Param("kw") String kw, @Param("limit") int limit);
}
