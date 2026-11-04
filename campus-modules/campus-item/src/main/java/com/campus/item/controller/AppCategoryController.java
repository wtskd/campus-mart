package com.campus.item.controller;

import com.campus.common.core.domain.Result;
import com.campus.item.dto.CategoryTreeVO;
import com.campus.item.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台商品分类接口
 *
 * @author campus-mart
 */
@Tag(name = "前台-商品分类", description = "用户端商品分类树")
@RestController
@RequestMapping("/item/app/category")
@RequiredArgsConstructor
public class AppCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树", description = "返回一级+二级分类（仅正常状态），Redis 缓存 30 分钟")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        return Result.ok(categoryService.appTree());
    }
}
