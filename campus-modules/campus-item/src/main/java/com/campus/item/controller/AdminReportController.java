package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.ReportHandleDTO;
import com.campus.item.dto.ReportQueryDTO;
import com.campus.item.dto.ReportVO;
import com.campus.item.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-举报管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-举报管理", description = "举报分页、举报处理（通过则商品置违规）")
@RestController
@RequestMapping("/item/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "举报分页", description = "按商品/举报人/状态/时间范围筛选")
    @RequiresPerm("item:report:list")
    @GetMapping("/page")
    public Result<PageResult<ReportVO>> page(@Valid ReportQueryDTO query) {
        return Result.ok(reportService.adminPage(query));
    }

    @Operation(summary = "处理举报", description = "status=1 通过（商品置 status=6 违规）、status=2 驳回")
    @RequiresPerm("item:report:handle")
    @OperLog(title = "举报处理", businessType = 2)
    @PutMapping("/handle")
    public Result<Void> handle(@Valid @RequestBody ReportHandleDTO dto) {
        reportService.handle(dto);
        return Result.ok("举报处理完成", null);
    }
}
