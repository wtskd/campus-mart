package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.item.dto.MessageQueryDTO;
import com.campus.item.dto.MessageVO;
import com.campus.item.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台-消息接口（需登录）
 *
 * <p>biz_message 只承载系统/业务通知（订单、审核、AI），不含用户私信。</p>
 *
 * @author campus-mart
 */
@Tag(name = "前台-消息", description = "我的消息列表、未读数、标记已读")
@RestController
@RequestMapping("/item/app/message")
@RequiredArgsConstructor
public class AppMessageController {

    private final MessageService messageService;

    @Operation(summary = "我的消息分页", description = "receiver=当前用户，可按消息类型与已读状态筛选；/my 为兼容别名")
    @GetMapping({"/list", "/my"})
    public Result<PageResult<MessageVO>> list(@Valid MessageQueryDTO query) {
        return Result.ok(messageService.myPage(query));
    }

    @Operation(summary = "未读消息数", description = "当前用户未读消息总数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount());
    }

    @Operation(summary = "标记单条已读", description = "只能标记自己的消息；已读时幂等返回")
    @PutMapping("/read/{messageId}")
    public Result<Void> read(@Parameter(description = "消息ID") @PathVariable("messageId") Long messageId) {
        messageService.read(messageId);
        return Result.ok("已标记为已读", null);
    }

    @Operation(summary = "全部标记已读", description = "把当前用户所有未读消息置为已读")
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok("全部已读", null);
    }
}
