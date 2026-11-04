package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.RoleMenuDTO;
import com.campus.system.domain.dto.RoleStatusDTO;
import com.campus.system.domain.entity.SysRole;
import com.campus.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
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
 * 角色管理。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:role:list"})
    public Result<PageResult<SysRole>> list(PageQuery pageQuery,
                                            @RequestParam(required = false) String roleName,
                                            @RequestParam(required = false) String roleKey,
                                            @RequestParam(required = false) String status) {
        return Result.ok(roleService.page(pageQuery, roleName, roleKey, status));
    }

    @Operation(summary = "角色分页列表（前端兼容路径，等价 /list）")
    @GetMapping("/page")
    @RequiresPerm({"system:role:list"})
    public Result<PageResult<SysRole>> page(PageQuery pageQuery,
                                            @RequestParam(required = false) String roleName,
                                            @RequestParam(required = false) String roleKey,
                                            @RequestParam(required = false) String status) {
        return list(pageQuery, roleName, roleKey, status);
    }

    @Operation(summary = "全部启用角色（下拉）")
    @GetMapping("/optionselect")
    @RequiresPerm({"system:role:list"})
    public Result<List<SysRole>> optionSelect() {
        return Result.ok(roleService.optionSelect());
    }

    @Operation(summary = "角色已授权菜单ID")
    @GetMapping("/{roleId}/menuIds")
    @RequiresPerm({"system:role:query", "system:role:list"})
    public Result<List<Long>> menuIds(@PathVariable Long roleId) {
        return Result.ok(roleService.menuIds(roleId));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{roleId}")
    @RequiresPerm({"system:role:query", "system:role:list"})
    public Result<SysRole> detail(@PathVariable Long roleId) {
        return Result.ok(roleService.detail(roleId));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @RequiresPerm({"system:role:add", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 1)
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.add(role);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改角色")
    @PutMapping
    @RequiresPerm({"system:role:edit", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 2)
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.update(role);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除角色（逗号分隔批量）")
    @DeleteMapping("/{roleIds}")
    @RequiresPerm({"system:role:remove", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 3)
    public Result<Void> remove(@PathVariable String roleIds) {
        roleService.remove(roleIds);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "修改角色状态（0正常 1停用）")
    @PutMapping("/changeStatus")
    @RequiresPerm({"system:role:edit", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 2)
    public Result<Void> changeStatus(@RequestBody RoleStatusDTO dto) {
        roleService.changeStatus(dto.getRoleId(), dto.getStatus());
        return Result.ok("状态修改成功", null);
    }

    @Operation(summary = "修改角色状态（前端兼容路径）")
    @PutMapping("/{roleId}/status/{status}")
    @RequiresPerm({"system:role:edit", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 2)
    public Result<Void> changeStatusCompat(@PathVariable Long roleId, @PathVariable String status) {
        roleService.changeStatus(roleId, status);
        return Result.ok("状态修改成功", null);
    }

    @Operation(summary = "角色菜单授权（整体重写）")
    @PutMapping("/menu")
    @RequiresPerm({"system:role:edit", "system:role:list"})
    @OperLog(title = "角色管理", businessType = 4)
    public Result<Void> assignMenus(@RequestBody RoleMenuDTO dto) {
        roleService.assignMenus(dto.getRoleId(), dto.getMenuIds());
        return Result.ok("授权成功", null);
    }
}
