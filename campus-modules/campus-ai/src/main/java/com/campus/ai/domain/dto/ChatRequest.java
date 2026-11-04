package com.campus.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能客服请求
 *
 * @author campus-mart
 */
@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题最多 500 字")
    private String question;

    /** 会话 ID（前端生成，用于多轮上下文，30 分钟有效） */
    @Size(max = 64, message = "sessionId 过长")
    private String sessionId;

    /** 关联商品 ID（可选，问答时补充商品信息） */
    private Long itemId;
}
