package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.entity.SysLoginLog;
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
 * 日志路径兼容层。
 *
 * <p>团队前端联调按若依习惯走 `/system/log/oper/**`、`/system/log/login/**`，
 * 而本模块正式契约是 `/system/operlog/**`、`/system/loginlog/**`；这里只做转发别名，
 * 避免联调阶段因为路径差异改前端或反复扯皮。正式接口仍以契约文档为准。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-日志兼容路径")
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SysLogCompatController {

    private final SysLogService logService;

    @Operation(summary = "操作日志分页（兼容路径）")
    @GetMapping("/oper/page")
    @RequiresPerm({"system:operlog:list"})
    public Result<PageResult<SysOperLog>> operPage(PageQuery pageQuery,
                                                   @RequestParam(required = false) String title,
                                                   @RequestParam(required = false) String operName,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String businessType,
                                                   @RequestParam(required = false) String beginTime,
                                                   @RequestParam(required = false) String endTime) {
        return Result.ok(logService.pageOper(pageQuery, title, operName, status, businessType, beginTime, endTime));
    }

    @Operation(summary = "删除操作日志（兼容路径）")
    @DeleteMapping("/oper/{operIds}")
    @RequiresPerm({"system:operlog:remove", "system:operlog:list"})
    @OperLog(title = "操作日志", businessType = 3)
    public Result<Void> removeOper(@PathVariable String operIds) {
        logService.removeOper(operIds);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "清空操作日志（兼容路径）")
    @DeleteMapping("/oper/clean")
    @RequiresPerm({"system:operlog:remove", "system:operlog:list"})
    @OperLog(title = "操作日志", businessType = 9)
    public Result<Void> cleanOper() {
        logService.cleanOper();
        return Result.ok("清空成功", null);
    }

    @Operation(summary = "登录日志分页（兼容路径）")
    @GetMapping("/login/page")
    @RequiresPerm({"system:loginlog:list"})
    public Result<PageResult<SysLoginLog>> loginPage(PageQuery pageQuery,
                                                     @RequestParam(required = false) String userName,
                                                     @RequestParam(required = false) String ipaddr,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String beginTime,
                                                     @RequestParam(required = false) String endTime) {
        return Result.ok(logService.pageLogin(pageQuery, userName, ipaddr, status, beginTime, endTime));
    }

    @Operation(summary = "删除登录日志（兼容路径）")
    @DeleteMapping("/login/{infoIds}")
    @RequiresPerm({"system:loginlog:remove", "system:loginlog:list"})
    @OperLog(title = "登录日志", businessType = 3)
    public Result<Void> removeLogin(@PathVariable String infoIds) {
        logService.removeLogin(infoIds);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "清空登录日志（兼容路径）")
    @DeleteMapping("/login/clean")
    @RequiresPerm({"system:loginlog:remove", "system:loginlog:list"})
    @OperLog(title = "登录日志", businessType = 9)
    public Result<Void> cleanLogin() {
        logService.cleanLogin();
        return Result.ok("清空成功", null);
    }
}
