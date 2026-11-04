package com.campus.ai.controller;

import com.campus.ai.service.ModerationService;
import com.campus.api.dto.ModerationDTO;
import com.campus.api.dto.ModerationRequest;
import com.campus.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 内容审核（服务间内部接口，item 服务调用）
 *
 * @author campus-mart
 */
@Tag(name = "AI 内容审核", description = "敏感词规则 + LLM 复核")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @Operation(summary = "商品内容审核（内部接口，item 服务调用）")
    @PostMapping({"/moderate", "/inner/moderate"})
    public Result<ModerationDTO> moderate(@RequestBody ModerationRequest request) {
        return Result.ok(moderationService.moderate(request));
    }
}
