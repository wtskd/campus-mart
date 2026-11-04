package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.entity.SysLoginLog;
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
 * 登录日志（只读 + 清理）。
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-登录日志")
@RestController
@RequestMapping("/system/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLogService logService;

    @Operation(summary = "登录日志分页列表（支持时间区间）")
    @GetMapping("/list")
    @RequiresPerm({"system:loginlog:list"})
    public Result<PageResult<SysLoginLog>> list(PageQuery pageQuery,
                                                @RequestParam(required = false) String userName,
                                                @RequestParam(required = false) String ipaddr,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String beginTime,
                                                @RequestParam(required = false) String endTime) {
        return Result.ok(logService.pageLogin(pageQuery, userName, ipaddr, status, beginTime, endTime));
    }

    @Operation(summary = "清空登录日志")
    @DeleteMapping("/clean")
    @RequiresPerm({"system:loginlog:remove", "system:loginlog:list"})
    @OperLog(title = "登录日志", businessType = 9)
    public Result<Void> clean() {
        logService.cleanLogin();
        return Result.ok("清空成功", null);
    }

    @Operation(summary = "删除登录日志（逗号分隔批量）")
    @DeleteMapping("/{infoIds}")
    @RequiresPerm({"system:loginlog:remove", "system:loginlog:list"})
    @OperLog(title = "登录日志", businessType = 3)
    public Result<Void> remove(@PathVariable String infoIds) {
        logService.removeLogin(infoIds);
        return Result.ok("删除成功", null);
    }
}
