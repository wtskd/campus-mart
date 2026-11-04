package com.campus.system.controller;

import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.TreeSelectVO;
import com.campus.system.domain.entity.SysDept;
import com.campus.system.service.SysDeptService;
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
 * 学院（部门）管理。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-学院管理")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @Operation(summary = "学院扁平列表")
    @GetMapping("/list")
    @RequiresPerm({"system:dept:list"})
    public Result<List<SysDept>> list(@RequestParam(required = false) String deptName,
                                      @RequestParam(required = false) String status) {
        return Result.ok(deptService.list(deptName, status));
    }

    @Operation(summary = "学院树（前端兼容路径，等价 /treeselect）")
    @GetMapping("/tree")
    @RequiresPerm({"system:dept:list"})
    public Result<List<TreeSelectVO>> tree() {
        return Result.ok(deptService.treeSelect());
    }

    @Operation(summary = "学院下拉树")
    @GetMapping("/treeselect")
    @RequiresPerm({"system:dept:list"})
    public Result<List<TreeSelectVO>> treeSelect() {
        return Result.ok(deptService.treeSelect());
    }

    @Operation(summary = "学院详情")
    @GetMapping("/{deptId}")
    @RequiresPerm({"system:dept:query", "system:dept:list"})
    public Result<SysDept> detail(@PathVariable Long deptId) {
        return Result.ok(deptService.detail(deptId));
    }

    @Operation(summary = "新增学院")
    @PostMapping
    @RequiresPerm({"system:dept:add", "system:dept:list"})
    @OperLog(title = "学院管理", businessType = 1)
    public Result<Void> add(@RequestBody SysDept dept) {
        deptService.add(dept);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改学院")
    @PutMapping
    @RequiresPerm({"system:dept:edit", "system:dept:list"})
    @OperLog(title = "学院管理", businessType = 2)
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除学院")
    @DeleteMapping("/{deptId}")
    @RequiresPerm({"system:dept:remove", "system:dept:list"})
    @OperLog(title = "学院管理", businessType = 3)
    public Result<Void> remove(@PathVariable Long deptId) {
        deptService.remove(deptId);
        return Result.ok("删除成功", null);
    }
}
