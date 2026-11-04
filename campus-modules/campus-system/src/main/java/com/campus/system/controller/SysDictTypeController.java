package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.entity.SysDictType;
import com.campus.system.service.SysDictService;
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
 * 字典类型管理。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-字典类型")
@RestController
@RequestMapping("/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final SysDictService dictService;

    @Operation(summary = "字典类型分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:dict:list"})
    public Result<PageResult<SysDictType>> list(PageQuery pageQuery,
                                                @RequestParam(required = false) String dictName,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String status) {
        return Result.ok(dictService.pageTypes(pageQuery, dictName, dictType, status));
    }

    @Operation(summary = "字典类型分页列表（前端兼容路径）")
    @GetMapping("/page")
    @RequiresPerm({"system:dict:list"})
    public Result<PageResult<SysDictType>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String dictName,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String status) {
        return list(pageQuery, dictName, dictType, status);
    }

    @Operation(summary = "全部启用字典类型（下拉）")
    @GetMapping("/optionselect")
    @RequiresPerm({"system:dict:list"})
    public Result<List<SysDictType>> optionSelect() {
        return Result.ok(dictService.optionTypes());
    }

    @Operation(summary = "字典类型详情")
    @GetMapping("/{dictId}")
    @RequiresPerm({"system:dict:query", "system:dict:list"})
    public Result<SysDictType> detail(@PathVariable Long dictId) {
        return Result.ok(dictService.getType(dictId));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping
    @RequiresPerm({"system:dict:add", "system:dict:list"})
    @OperLog(title = "字典类型", businessType = 1)
    public Result<Void> add(@RequestBody SysDictType type) {
        dictService.addType(type);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改字典类型")
    @PutMapping
    @RequiresPerm({"system:dict:edit", "system:dict:list"})
    @OperLog(title = "字典类型", businessType = 2)
    public Result<Void> update(@RequestBody SysDictType type) {
        dictService.updateType(type);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除字典类型（级联删除字典数据）")
    @DeleteMapping("/{dictIds}")
    @RequiresPerm({"system:dict:remove", "system:dict:list"})
    @OperLog(title = "字典类型", businessType = 3)
    public Result<Void> remove(@PathVariable String dictIds) {
        dictService.removeTypes(dictIds);
        return Result.ok("删除成功", null);
    }
}
