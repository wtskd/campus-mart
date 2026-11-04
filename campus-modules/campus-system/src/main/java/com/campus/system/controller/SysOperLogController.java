package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.entity.SysOperLog;
import com.campus.system.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志（只读 + 清理）。
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-操作日志")
@RestController
@RequestMapping("/system/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysLogService logService;

    @Operation(summary = "操作日志分页列表（支持时间区间）")
    @GetMapping("/list")
    @RequiresPerm({"system:operlog:list"})
    public Result<PageResult<SysOperLog>> list(PageQuery pageQuery,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) String operName,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String businessType,
                                               @RequestParam(required = false) String beginTime,
                                               @RequestParam(required = false) String endTime) {
        return Result.ok(logService.pageOper(pageQuery, title, operName, status, businessType, beginTime, endTime));
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/clean")
    @RequiresPerm({"system:operlog:remove", "system:operlog:list"})
    @OperLog(title = "操作日志", businessType = 9)
    public Result<Void> clean() {
        logService.cleanOper();
        return Result.ok("清空成功", null);
    }

    @Operation(summary = "删除操作日志（逗号分隔批量）")
    @DeleteMapping("/{operIds}")
    @RequiresPerm({"system:operlog:remove", "system:operlog:list"})
    @OperLog(title = "操作日志", businessType = 3)
    public Result<Void> remove(@PathVariable String operIds) {
        logService.removeOper(operIds);
        return Result.ok("删除成功", null);
    }
}
