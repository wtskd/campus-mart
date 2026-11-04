package com.campus.item.controller;

import com.campus.common.core.domain.Result;
import com.campus.item.domain.entity.BizBanner;
import com.campus.item.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台-轮播接口
 *
 * @author campus-mart
 */
@Tag(name = "前台-轮播", description = "首页轮播列表")
@RestController
@RequestMapping("/item/app/banner")
@RequiredArgsConstructor
public class AppBannerController {

    private final BannerService bannerService;

    @Operation(summary = "轮播列表", description = "仅返回上架且在生效时间内的轮播，按 sort 升序")
    @GetMapping("/list")
    public Result<List<BizBanner>> list() {
        return Result.ok(bannerService.appList());
    }
}
