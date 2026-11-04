package com.campus.item.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.item.dto.CommentCreateDTO;
import com.campus.item.dto.CommentCreateVO;
import com.campus.item.dto.CommentVO;
import com.campus.item.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台商品评价接口
 *
 * <p>任务书路径为 {@code /item/app/comment}，同时兼容 lead 裁决中的 {@code /item/comment} 路径。</p>
 *
 * @author campus-mart
 */
@Tag(name = "前台-商品评价", description = "商品评价列表与发表评价")
@RestController
@RequestMapping({"/item/app/comment", "/item/comment"})
@RequiredArgsConstructor
public class AppCommentController {

    private final CommentService commentService;

    @Operation(summary = "商品评价列表", description = "分页返回正常状态（status='0'）的评价，含评价人昵称与评分")
    @GetMapping("/list/{itemId}")
    public Result<PageResult<CommentVO>> list(
            @Parameter(description = "商品ID") @PathVariable("itemId") Long itemId, PageQuery query) {
        return Result.ok(commentService.pageByItem(itemId, query));
    }

    @Operation(summary = "发表评价", description = "需登录；orderId 必填，仅能评价自己已完成且未评价过的订单")
    @PostMapping
    public Result<CommentCreateVO> create(@Valid @RequestBody CommentCreateDTO dto) {
        return Result.ok("评价成功", commentService.create(dto));
    }
}
