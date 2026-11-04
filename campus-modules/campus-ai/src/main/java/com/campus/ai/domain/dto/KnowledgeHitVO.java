package com.campus.ai.domain.dto;

import lombok.Data;

/**
 * 知识库召回命中条目
 *
 * @author campus-mart
 */
@Data
public class KnowledgeHitVO {

    private Long knowledgeId;

    private String kbType;

    private String title;

    private String content;

    private String keywords;

    /** 相关度得分 */
    private Double score;
}
