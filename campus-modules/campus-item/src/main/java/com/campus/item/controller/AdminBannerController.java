package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.domain.entity.BizBanner;
import com.campus.item.dto.BannerQueryDTO;
import com.campus.item.dto.BannerSaveDTO;
import com.campus.item.service.BannerService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台-轮播管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-轮播管理", description = "轮播分页、新增、修改、删除")
@RestController
@RequestMapping("/item/banner")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播分页", description = "按标题/状态筛选，按 sort 升序")
    @RequiresPerm("item:banner:list")
    @GetMapping("/page")
    public Result<PageResult<BizBanner>> page(@Valid BannerQueryDTO query) {
        return Result.ok(bannerService.adminPage(query));
    }

    @Operation(summary = "轮播列表", description = "后台不分页列表（含下架），按 sort 升序，供下拉/预览使用")
    @RequiresPerm("item:banner:list")
    @GetMapping("/list")
    public Result<List<BizBanner>> list() {
        return Result.ok(bannerService.adminList());
    }

    @Operation(summary = "新增轮播", description = "status='0' 上架 / '1' 下架；生效结束时间必须晚于开始时间")
    @RequiresPerm("item:banner:add")
    @OperLog(title = "轮播管理", businessType = 1)
    @PostMapping("")
    public Result<Long> create(@Valid @RequestBody BannerSaveDTO dto) {
        return Result.ok("新增成功", bannerService.create(dto));
    }

    @Operation(summary = "修改轮播", description = "bannerId 必填")
    @RequiresPerm("item:banner:edit")
    @OperLog(title = "轮播管理", businessType = 2)
    @PutMapping("")
    public Result<Void> update(@Valid @RequestBody BannerSaveDTO dto) {
        bannerService.update(dto);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除轮播")
    @RequiresPerm("item:banner:remove")
    @OperLog(title = "轮播管理", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "轮播ID") @PathVariable("id") Long bannerId) {
        bannerService.delete(bannerId);
        return Result.ok("删除成功", null);
    }
}
