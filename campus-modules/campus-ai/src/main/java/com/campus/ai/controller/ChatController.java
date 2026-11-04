package com.campus.ai.controller;

import com.campus.ai.domain.dto.ChatRequest;
import com.campus.ai.domain.dto.ChatResponse;
import com.campus.ai.service.ChatService;
import com.campus.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能客服
 *
 * @author campus-mart
 */
@Tag(name = "AI 智能客服", description = "基于知识库的 RAG 问答")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "智能客服问答（RAG 知识库召回）")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request));
    }
}
