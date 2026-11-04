package com.campus.item.controller;

import com.campus.common.core.domain.Result;
import com.campus.item.dto.ReportCreateDTO;
import com.campus.item.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台-举报接口（需登录）
 *
 * @author campus-mart
 */
@Tag(name = "前台-举报", description = "举报商品（暂只支持 targetType=item），同一用户同目标 24 小时内只能举报一次")
@RestController
@RequestMapping("/item/app/report")
@RequiredArgsConstructor
public class AppReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报", description = "targetType 仅支持 item（评论/用户暂不支持）；重复举报 24 小时限制")
    @PostMapping("")
    public Result<Long> create(@Valid @RequestBody ReportCreateDTO dto) {
        return Result.ok("举报已提交，我们会尽快处理", reportService.create(dto));
    }
}
