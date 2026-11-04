package com.campus.system.controller;

import com.campus.api.dto.MenuNodeDTO;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.RoleMenuTreeVO;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysMenu;
import com.campus.system.service.SysMenuService;
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
 * 菜单管理 + 动态路由。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单扁平列表（前端自行建树，带 parentId）")
    @GetMapping("/list")
    @RequiresPerm({"system:menu:list"})
    public Result<List<SysMenu>> list(@RequestParam(required = false) String menuName,
                                      @RequestParam(required = false) String status) {
        return Result.ok(menuService.list(menuName, status));
    }

    @Operation(summary = "菜单树（菜单管理页表格树）")
    @GetMapping("/tree")
    @RequiresPerm({"system:menu:list"})
    public Result<List<MenuNodeDTO>> tree() {
        return Result.ok(menuService.allTree());
    }

    @Operation(summary = "菜单下拉树（含按钮，角色授权勾选）")
    @GetMapping("/treeselect")
    @RequiresPerm({"system:menu:list"})
    public Result<List<TreeSelectVO>> treeSelect() {
        return Result.ok(menuService.treeSelect());
    }

    @Operation(summary = "角色菜单授权树（菜单树 + 已勾选ID）")
    @GetMapping("/roleMenuTreeselect/{roleId}")
    @RequiresPerm({"system:menu:list", "system:role:list"})
    public Result<RoleMenuTreeVO> roleMenuTree(@PathVariable Long roleId) {
        return Result.ok(menuService.roleMenuTree(roleId));
    }

    @Operation(summary = "当前登录用户动态路由")
    @GetMapping("/routers")
    public Result<List<MenuNodeDTO>> routers() {
        return Result.ok(menuService.routers());
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/{menuId}")
    @RequiresPerm({"system:menu:query", "system:menu:list"})
    public Result<SysMenu> detail(@PathVariable Long menuId) {
        return Result.ok(menuService.detail(menuId));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @RequiresPerm({"system:menu:add", "system:menu:list"})
    @OperLog(title = "菜单管理", businessType = 1)
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.add(menu);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改菜单")
    @PutMapping
    @RequiresPerm({"system:menu:edit", "system:menu:list"})
    @OperLog(title = "菜单管理", businessType = 2)
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{menuId}")
    @RequiresPerm({"system:menu:remove", "system:menu:list"})
    @OperLog(title = "菜单管理", businessType = 3)
    public Result<Void> remove(@PathVariable Long menuId) {
        menuService.remove(menuId);
        return Result.ok("删除成功", null);
    }

    /* ------------------- 内部接口：campus-auth 通过 Feign 调用 ------------------- */

    @Operation(summary = "内部-用户菜单树")
    @GetMapping("/inner/tree/{userId}")
    public Result<List<MenuNodeDTO>> innerTree(@PathVariable Long userId) {
        return Result.ok(menuService.userMenuTree(userId));
    }
}
