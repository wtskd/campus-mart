package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.MessageQueryDTO;
import com.campus.item.dto.MessageVO;
import com.campus.item.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-消息管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-消息管理", description = "全量消息分页（按接收人/类型/已读/时间筛选）")
@RestController
@RequestMapping("/item/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    @Operation(summary = "消息分页", description = "管理员查看全量系统/业务通知")
    @RequiresPerm("item:message:list")
    @GetMapping("/page")
    public Result<PageResult<MessageVO>> page(@Valid MessageQueryDTO query) {
        return Result.ok(messageService.adminPage(query));
    }
}
