package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.domain.entity.BizNotice;
import com.campus.item.dto.NoticeQueryDTO;
import com.campus.item.dto.NoticeSaveDTO;
import com.campus.item.service.NoticeService;
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

/**
 * 后台-公告管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-公告管理", description = "公告分页、新增、修改、删除")
@RestController
@RequestMapping("/item/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "公告分页", description = "按标题/类型/状态/发布时间范围筛选")
    @RequiresPerm("item:notice:list")
    @GetMapping("/page")
    public Result<PageResult<BizNotice>> page(@Valid NoticeQueryDTO query) {
        return Result.ok(noticeService.adminPage(query));
    }

    @Operation(summary = "公告详情", description = "后台编辑回显用，草稿也可查")
    @RequiresPerm("item:notice:list")
    @GetMapping("/{id}")
    public Result<BizNotice> detail(@Parameter(description = "公告ID") @PathVariable("id") Long noticeId) {
        return Result.ok(noticeService.adminDetail(noticeId));
    }

    @Operation(summary = "新增公告", description = "status='0' 发布（无发布时间时取当前时间），status='1' 草稿")
    @RequiresPerm("item:notice:add")
    @OperLog(title = "公告管理", businessType = 1)
    @PostMapping("")
    public Result<Long> create(@Valid @RequestBody NoticeSaveDTO dto) {
        return Result.ok("新增成功", noticeService.create(dto));
    }

    @Operation(summary = "修改公告", description = "noticeId 必填")
    @RequiresPerm("item:notice:edit")
    @OperLog(title = "公告管理", businessType = 2)
    @PutMapping("")
    public Result<Void> update(@Valid @RequestBody NoticeSaveDTO dto) {
        noticeService.update(dto);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除公告")
    @RequiresPerm("item:notice:remove")
    @OperLog(title = "公告管理", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "公告ID") @PathVariable("id") Long noticeId) {
        noticeService.delete(noticeId);
        return Result.ok("删除成功", null);
    }
}
