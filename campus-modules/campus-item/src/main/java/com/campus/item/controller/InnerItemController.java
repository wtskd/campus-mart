package com.campus.item.controller;

import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.domain.Result;
import com.campus.item.service.FavoriteService;
import com.campus.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部接口（供 ai / analysis 等微服务通过 ItemClient 调用，契约固定不可变更）
 *
 * @author campus-mart
 */
@Tag(name = "内部接口-商品", description = "服务间调用，不对外暴露")
@RestController
@RequestMapping("/item/inner")
@RequiredArgsConstructor
public class InnerItemController {

    private final ItemService itemService;

    private final FavoriteService favoriteService;

    @Operation(summary = "商品概要", description = "含分类名、卖家昵称")
    @GetMapping("/item/{itemId}")
    public Result<ItemBriefDTO> getItem(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        return Result.ok(itemService.getItemBrief(itemId));
    }

    @Operation(summary = "在售商品列表", description = "AI 推荐候选集，默认最多 200 条，按浏览量倒序")
    @GetMapping("/item/on-sale")
    public Result<List<ItemBriefDTO>> listOnSale(
            @Parameter(description = "返回条数，最大 200") @RequestParam(value = "limit", defaultValue = "200") Integer limit) {
        return Result.ok(itemService.listOnSaleBrief(limit));
    }

    @Operation(summary = "用户收藏商品列表", description = "最多 200 条")
    @GetMapping("/item/favorite/{userId}")
    public Result<List<ItemBriefDTO>> listFavorite(
            @Parameter(description = "用户ID") @PathVariable("userId") Long userId) {
        return Result.ok(favoriteService.listFavoriteItems(userId));
    }
}
