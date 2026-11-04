package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.CommentAuditDTO;
import com.campus.item.dto.CommentQueryDTO;
import com.campus.item.dto.CommentVO;
import com.campus.item.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台商品评价管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-评价管理", description = "评价审核与删除")
@RestController
@RequestMapping("/item/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "评价分页", description = "可按商品/评价人/状态/内容关键字筛选")
    @RequiresPerm({"item:comment:list"})
    @GetMapping("/page")
    public Result<PageResult<CommentVO>> page(@Valid CommentQueryDTO query) {
        return Result.ok(commentService.pageAdmin(query));
    }

    @Operation(summary = "评价审核", description = "status=0 通过（正常显示），status=1 隐藏")
    @RequiresPerm({"item:comment:audit", "item:comment:edit", "item:comment:list"})
    @OperLog(title = "评价审核", businessType = 2)
    @PutMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody CommentAuditDTO dto) {
        commentService.audit(dto);
        return Result.ok("审核完成", null);
    }

    @Operation(summary = "删除评价")
    @RequiresPerm({"item:comment:remove", "item:comment:list"})
    @OperLog(title = "评价删除", businessType = 3)
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@Parameter(description = "评价ID") @PathVariable("commentId") Long commentId) {
        commentService.delete(commentId);
        return Result.ok("删除成功", null);
    }
}
