package com.campus.ai.controller;

import com.campus.ai.domain.dto.RecommendVO;
import com.campus.ai.service.RecommendService;
import com.campus.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 个性化推荐 / 相似商品
 *
 * @author campus-mart
 */
@Tag(name = "AI 推荐", description = "个性化推荐与相似商品")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "个性化推荐（ItemCF + 分类/价格画像，冷启动走热度）")
    @GetMapping("/recommend")
    public Result<List<RecommendVO>> recommend(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.ok(recommendService.recommend(userId, limit));
    }

    @Operation(summary = "相似商品（同分类 + 价格 ±50%）")
    @GetMapping("/similar/{itemId}")
    public Result<List<RecommendVO>> similar(
            @PathVariable("itemId") Long itemId,
            @RequestParam(value = "limit", defaultValue = "6") Integer limit) {
        return Result.ok(recommendService.similar(itemId, limit));
    }
}
