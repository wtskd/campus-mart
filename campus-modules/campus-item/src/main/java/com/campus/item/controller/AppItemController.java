package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.item.dto.AdminItemQueryDTO;
import com.campus.item.dto.AppItemQueryDTO;
import com.campus.item.dto.ItemDetailVO;
import com.campus.item.dto.ItemListVO;
import com.campus.item.dto.ItemPublishDTO;
import com.campus.item.dto.ItemPublishVO;
import com.campus.item.dto.ItemUpdateDTO;
import com.campus.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台商品接口
 *
 * @author campus-mart
 */
@Tag(name = "前台-商品", description = "用户端商品浏览、发布、编辑、下架")
@RestController
@RequestMapping("/item/app/item")
@RequiredArgsConstructor
public class AppItemController {

    private final ItemService itemService;

    @Operation(summary = "在售商品分页", description = "只返回在售（status=2）商品，支持关键字/分类/价格/成色筛选与排序")
    @GetMapping("/page")
    public Result<PageResult<ItemListVO>> page(@Valid AppItemQueryDTO query) {
        return Result.ok(itemService.pageAppItems(query));
    }

    @Operation(summary = "商品详情", description = "浏览量 +1，返回卖家信息与当前用户是否已收藏")
    @GetMapping("/{itemId}")
    public Result<ItemDetailVO> detail(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        return Result.ok(itemService.getDetail(itemId));
    }

    @Operation(summary = "发布商品", description = "需登录；发布时调用 AI 内容审核，不通过则直接置为违规状态")
    @PostMapping
    public Result<ItemPublishVO> publish(@Valid @RequestBody ItemPublishDTO dto) {
        return Result.ok("发布成功", itemService.publish(dto));
    }

    @Operation(summary = "编辑自己的商品", description = "仅能编辑自己的商品，且状态只能是待审核/在售/已下架；标题或描述变更后重新审核")
    @PutMapping
    public Result<ItemPublishVO> update(@Valid @RequestBody ItemUpdateDTO dto) {
        return Result.ok("修改成功", itemService.update(dto));
    }

    @Operation(summary = "下架商品", description = "仅能下架自己的在售商品（status 2 → 5）")
    @PostMapping("/{itemId}/off-shelf")
    public Result<Void> offShelf(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        itemService.offShelf(itemId);
        return Result.ok("下架成功", null);
    }

    @Operation(summary = "我发布的商品", description = "按状态筛选，分页返回")
    @GetMapping("/my")
    public Result<PageResult<ItemListVO>> my(@Valid AdminItemQueryDTO query) {
        return Result.ok(itemService.pageMyItems(query));
    }

    @Operation(summary = "推荐商品（兜底）", description = "个性化推荐由 ai 服务直连提供；本接口按浏览量+发布时间返回热门在售商品")
    @GetMapping("/recommend")
    public Result<List<ItemListVO>> recommend(
            @Parameter(description = "返回条数，最大 20") @RequestParam(value = "limit", defaultValue = "8") Integer limit) {
        return Result.ok(itemService.recommend(limit));
    }
}
