package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.dto.DictOptionVO;
import com.campus.system.domain.entity.SysDictData;
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
 * 字典数据管理。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-字典数据")
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictService dictService;

    @Operation(summary = "字典数据分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:dict:list"})
    public Result<PageResult<SysDictData>> list(PageQuery pageQuery,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String dictLabel,
                                                @RequestParam(required = false) String status) {
        return Result.ok(dictService.pageData(pageQuery, dictType, dictLabel, status));
    }

    @Operation(summary = "字典数据分页列表（前端兼容路径）")
    @GetMapping("/page")
    @RequiresPerm({"system:dict:list"})
    public Result<PageResult<SysDictData>> page(PageQuery pageQuery,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String dictLabel,
                                                @RequestParam(required = false) String status) {
        return list(pageQuery, dictType, dictLabel, status);
    }

    /**
     * 前台/后台下拉数据源。
     *
     * <p>不挂按钮权限：字典是纯展示数据，登录即可读；写权限在类型/数据接口上控制。</p>
     */
    @Operation(summary = "按字典类型取下拉项（走 Redis 缓存）")
    @GetMapping("/type/{dictType}")
    public Result<List<DictOptionVO>> dataByType(@PathVariable String dictType) {
        return Result.ok(dictService.dataByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @GetMapping("/{dictCode}")
    @RequiresPerm({"system:dict:query", "system:dict:list"})
    public Result<SysDictData> detail(@PathVariable Long dictCode) {
        return Result.ok(dictService.getData(dictCode));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping
    @RequiresPerm({"system:dict:add", "system:dict:list"})
    @OperLog(title = "字典数据", businessType = 1)
    public Result<Void> add(@RequestBody SysDictData data) {
        dictService.addData(data);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改字典数据")
    @PutMapping
    @RequiresPerm({"system:dict:edit", "system:dict:list"})
    @OperLog(title = "字典数据", businessType = 2)
    public Result<Void> update(@RequestBody SysDictData data) {
        dictService.updateData(data);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除字典数据（逗号分隔批量）")
    @DeleteMapping("/{dictCodes}")
    @RequiresPerm({"system:dict:remove", "system:dict:list"})
    @OperLog(title = "字典数据", businessType = 3)
    public Result<Void> remove(@PathVariable String dictCodes) {
        dictService.removeData(dictCodes);
        return Result.ok("删除成功", null);
    }
}
