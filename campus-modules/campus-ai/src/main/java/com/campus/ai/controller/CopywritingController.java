package com.campus.ai.controller;

import com.campus.ai.domain.dto.CopywritingRequest;
import com.campus.ai.domain.dto.CopywritingResponse;
import com.campus.ai.domain.dto.SummaryResponse;
import com.campus.ai.service.CopywritingService;
import com.campus.ai.service.SummaryService;
import com.campus.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 发布文案 / 商品摘要
 *
 * @author campus-mart
 */
@Tag(name = "AI 文案生成", description = "一键发布文案、商品卖点摘要")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class CopywritingController {

    private final CopywritingService copywritingService;

    private final SummaryService summaryService;

    @Operation(summary = "一键生成商品发布文案")
    @PostMapping({"/copywrite", "/copywriting/item"})
    public Result<CopywritingResponse> copywriting(@Valid @RequestBody CopywritingRequest request) {
        return Result.ok(copywritingService.generate(request));
    }

    @Operation(summary = "生成商品卖点摘要")
    @PostMapping("/summary/item/{itemId}")
    public Result<SummaryResponse> summary(@PathVariable("itemId") Long itemId) {
        return Result.ok(summaryService.summarize(itemId));
    }
}
