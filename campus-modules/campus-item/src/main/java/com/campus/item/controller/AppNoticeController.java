package com.campus.item.controller;

import com.campus.common.core.domain.Result;
import com.campus.item.domain.entity.BizNotice;
import com.campus.item.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台-公告接口
 *
 * @author campus-mart
 */
@Tag(name = "前台-公告", description = "公告列表（发布中）与公告详情")
@RestController
@RequestMapping("/item/app/notice")
@RequiredArgsConstructor
public class AppNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "公告列表", description = "仅返回已发布且已到发布时间的公告，按发布时间倒序，最多 20 条")
    @GetMapping("/list")
    public Result<List<BizNotice>> list() {
        return Result.ok(noticeService.appList());
    }

    @Operation(summary = "公告详情", description = "未发布/未到发布时间的公告返回 404")
    @GetMapping("/{noticeId}")
    public Result<BizNotice> detail(@Parameter(description = "公告ID") @PathVariable("noticeId") Long noticeId) {
        return Result.ok(noticeService.appDetail(noticeId));
    }
}
