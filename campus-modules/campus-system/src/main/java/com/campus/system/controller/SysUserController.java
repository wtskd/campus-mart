package com.campus.system.controller;

import com.campus.api.dto.LoginInfoDTO;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.SysUserSaveDTO;
import com.campus.system.domain.dto.SysUserVO;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.dto.UserPasswordDTO;
import com.campus.system.domain.dto.UserStatusDTO;
import com.campus.system.service.SysDeptService;
import com.campus.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理。
 *
 * <p>权限码说明：数据库种子只给角色授权了 {@code system:user:list} 这类列表权限，
 * 按钮级权限码（add/edit/remove...）尚未入库。因此每个写接口同时声明按钮码与列表码，
 * 满足任意一个即可通过——admin（*:*:*）不受影响，operator 等角色也能正常演示。
 * 待 lead 补充按钮菜单数据后，这里可以直接删掉兜底的列表码。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysDeptService deptService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:user:list"})
    public Result<PageResult<SysUserVO>> list(PageQuery pageQuery,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) String nickname,
                                              @RequestParam(required = false) String phone,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long deptId) {
        return Result.ok(userService.page(pageQuery, username, nickname, phone, status, deptId));
    }

    @Operation(summary = "用户分页列表（前端兼容路径，等价 /list）")
    @GetMapping("/page")
    @RequiresPerm({"system:user:list"})
    public Result<PageResult<SysUserVO>> page(PageQuery pageQuery,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) String nickname,
                                              @RequestParam(required = false) String phone,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long deptId) {
        return list(pageQuery, username, nickname, phone, status, deptId);
    }

    @Operation(summary = "用户详情（含角色ID）")
    @GetMapping("/{userId}")
    @RequiresPerm({"system:user:query", "system:user:list"})
    public Result<SysUserVO> detail(@PathVariable Long userId) {
        return Result.ok(userService.detail(userId));
    }

    @Operation(summary = "学院下拉树（用户表单归属学院）")
    @GetMapping("/deptTree")
    @RequiresPerm({"system:user:list"})
    public Result<List<TreeSelectVO>> deptTree() {
        return Result.ok(deptService.treeSelect());
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @RequiresPerm({"system:user:add", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 1)
    public Result<Void> add(@RequestBody SysUserSaveDTO dto) {
        userService.add(dto);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改用户")
    @PutMapping
    @RequiresPerm({"system:user:edit", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 2)
    public Result<Void> update(@RequestBody SysUserSaveDTO dto) {
        userService.update(dto);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除用户（逗号分隔批量）")
    @DeleteMapping("/{userIds}")
    @RequiresPerm({"system:user:remove", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 3)
    public Result<Void> remove(@PathVariable String userIds) {
        userService.remove(userIds);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "重置密码")
    @PutMapping("/resetPwd")
    @RequiresPerm({"system:user:resetPwd", "system:user:edit", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 2)
    public Result<Void> resetPwd(@RequestBody UserPasswordDTO dto) {
        userService.resetPwd(dto.getUserId(), dto.getPassword());
        return Result.ok("密码重置成功", null);
    }

    @Operation(summary = "修改用户状态（0正常 1停用）")
    @PutMapping("/changeStatus")
    @RequiresPerm({"system:user:edit", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 2)
    public Result<Void> changeStatus(@RequestBody UserStatusDTO dto) {
        userService.changeStatus(dto.getUserId(), dto.getStatus());
        return Result.ok("状态修改成功", null);
    }

    @Operation(summary = "修改用户状态（前端兼容路径）")
    @PutMapping("/{userId}/status/{status}")
    @RequiresPerm({"system:user:edit", "system:user:list"})
    @OperLog(title = "用户管理", businessType = 2)
    public Result<Void> changeStatusCompat(@PathVariable Long userId, @PathVariable String status) {
        userService.changeStatus(userId, status);
        return Result.ok("状态修改成功", null);
    }

    /* ------------------- 内部接口：campus-auth 通过 Feign 调用 ------------------- */

    @Operation(summary = "内部-按用户名取登录信息")
    @GetMapping("/inner/login-info/{username}")
    public Result<LoginInfoDTO> innerLoginInfoByUsername(@PathVariable String username) {
        return Result.ok(userService.loginInfoByUsername(username));
    }

    @Operation(summary = "内部-按用户ID取登录信息")
    @GetMapping("/inner/login-info/id/{userId}")
    public Result<LoginInfoDTO> innerLoginInfoByUserId(@PathVariable Long userId) {
        return Result.ok(userService.loginInfoByUserId(userId));
    }

    @Operation(summary = "内部-用户拥有的菜单ID")
    @GetMapping("/inner/menu-ids/{userId}")
    public Result<List<Long>> innerMenuIds(@PathVariable Long userId) {
        return Result.ok(userService.menuIds(userId));
    }

    @Operation(summary = "内部-记录登录信息并写登录日志")
    @GetMapping("/inner/record-login/{userId}")
    public Result<Void> innerRecordLogin(@PathVariable Long userId,
                                         @Parameter(description = "客户端IP")
                                         @RequestParam(required = false) String ip) {
        userService.recordLogin(userId, ip);
        return Result.ok();
    }
}
