package com.campus.ai.controller;

import com.campus.ai.domain.dto.PriceRequest;
import com.campus.ai.domain.dto.PriceResponse;
import com.campus.ai.service.PriceService;
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
 * 场景五：智能估价
 *
 * @author campus-mart
 */
@Tag(name = "AI 智能估价")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @Operation(summary = "生成建议售价区间")
    @PostMapping("/price")
    public Result<PriceResponse> price(@Valid @RequestBody PriceRequest request) {
        return Result.ok(priceService.estimate(request));
    }
}
