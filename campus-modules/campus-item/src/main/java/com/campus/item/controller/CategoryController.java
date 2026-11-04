package com.campus.item.controller;

import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.CategorySaveDTO;
import com.campus.item.dto.CategoryStatusDTO;
import com.campus.item.dto.CategoryTreeVO;
import com.campus.item.dto.CategoryUpdateDTO;
import com.campus.item.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 后台商品分类接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-商品分类", description = "商品分类管理")
@RestController
@RequestMapping("/item/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表", description = "tree=true 返回树结构，false 返回平铺列表")
    @RequiresPerm({"item:category:list"})
    @GetMapping("/list")
    public Result<List<CategoryTreeVO>> list(
            @Parameter(description = "是否返回树结构") @RequestParam(value = "tree", defaultValue = "false") boolean tree) {
        return Result.ok(categoryService.adminList(tree));
    }

    @Operation(summary = "新增分类")
    @RequiresPerm({"item:category:add", "item:category:list"})
    @OperLog(title = "商品分类", businessType = 1)
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CategorySaveDTO dto) {
        categoryService.add(dto);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "编辑分类")
    @RequiresPerm({"item:category:edit", "item:category:list"})
    @OperLog(title = "商品分类", businessType = 2)
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CategoryUpdateDTO dto) {
        categoryService.update(dto);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除分类", description = "分类下存在子分类或商品时拒绝删除")
    @RequiresPerm({"item:category:remove", "item:category:list"})
    @OperLog(title = "商品分类", businessType = 3)
    @DeleteMapping("/{categoryId}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable("categoryId") Long categoryId) {
        categoryService.delete(categoryId);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "修改分类状态")
    @RequiresPerm({"item:category:edit", "item:category:list"})
    @OperLog(title = "商品分类", businessType = 2)
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody CategoryStatusDTO dto) {
        categoryService.updateStatus(dto);
        return Result.ok("状态修改成功", null);
    }
}
