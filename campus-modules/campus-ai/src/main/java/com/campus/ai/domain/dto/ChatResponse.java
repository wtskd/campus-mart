package com.campus.ai.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能客服响应
 *
 * @author campus-mart
 */
@Data
public class ChatResponse {

    private String answer;

    /** 会话ID：多轮上下文，前端原样回传即可延续对话 */
    private String sessionId;

    /** 命中的知识库条目 */
    private List<Source> sources = new ArrayList<>();

    /** 实际生成方 remote / local / mock */
    private String provider;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 推荐追问 */
    private List<String> suggestions = new ArrayList<>();

    @Data
    public static class Source {
        private Long knowledgeId;
        private String title;
        private Double score;

        public Source() {
        }

        public Source(Long knowledgeId, String title, Double score) {
            this.knowledgeId = knowledgeId;
            this.title = title;
            this.score = score;
        }
    }
}
