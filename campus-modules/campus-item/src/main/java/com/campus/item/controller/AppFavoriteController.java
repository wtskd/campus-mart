package com.campus.item.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.item.dto.FavoriteItemVO;
import com.campus.item.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台商品收藏接口
 *
 * @author campus-mart
 */
@Tag(name = "前台-商品收藏", description = "收藏、取消收藏、我的收藏")
@RestController
@RequestMapping("/item/app/favorite")
@RequiredArgsConstructor
public class AppFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏商品", description = "需登录；重复收藏幂等不报错")
    @PostMapping("/{itemId}")
    public Result<Void> add(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        favoriteService.add(itemId);
        return Result.ok("收藏成功", null);
    }

    @Operation(summary = "取消收藏", description = "需登录；未收藏时幂等返回")
    @DeleteMapping("/{itemId}")
    public Result<Void> remove(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        favoriteService.remove(itemId);
        return Result.ok("已取消收藏", null);
    }

    @Operation(summary = "我的收藏", description = "分页返回收藏的商品摘要")
    @GetMapping("/my")
    public Result<PageResult<FavoriteItemVO>> my(PageQuery query) {
        return Result.ok(favoriteService.pageMyFavorites(query));
    }

    @Operation(summary = "是否已收藏", description = "未登录返回 false")
    @GetMapping("/check/{itemId}")
    public Result<Boolean> check(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        return Result.ok(favoriteService.check(itemId));
    }
}
