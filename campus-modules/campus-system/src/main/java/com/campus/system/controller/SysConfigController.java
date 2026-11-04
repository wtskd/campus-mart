package com.campus.system.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.system.domain.entity.SysConfig;
import com.campus.system.service.SysConfigService;
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

/**
 * 参数配置管理。
 *
 * <p>权限码写法见 {@link SysUserController} 的说明：按钮码 + 列表码兜底。</p>
 *
 * @author campus-mart
 */
@Tag(name = "系统管理-参数配置")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @Operation(summary = "参数分页列表")
    @GetMapping("/list")
    @RequiresPerm({"system:config:list"})
    public Result<PageResult<SysConfig>> list(PageQuery pageQuery,
                                              @RequestParam(required = false) String configName,
                                              @RequestParam(required = false) String configKey,
                                              @RequestParam(required = false) String configType) {
        return Result.ok(configService.page(pageQuery, configName, configKey, configType));
    }

    @Operation(summary = "参数分页列表（前端兼容路径）")
    @GetMapping("/page")
    @RequiresPerm({"system:config:list"})
    public Result<PageResult<SysConfig>> page(PageQuery pageQuery,
                                              @RequestParam(required = false) String configName,
                                              @RequestParam(required = false) String configKey,
                                              @RequestParam(required = false) String configType) {
        return list(pageQuery, configName, configKey, configType);
    }

    @Operation(summary = "按参数键名取值（走 Redis 缓存；不存在返回 null）")
    @GetMapping("/configKey/{configKey}")
    public Result<String> valueByKey(@PathVariable String configKey) {
        return Result.ok(configService.valueByKey(configKey));
    }

    @Operation(summary = "按参数键名取值（前端兼容路径）")
    @GetMapping("/key/{configKey}")
    public Result<String> valueByKeyCompat(@PathVariable String configKey) {
        return Result.ok(configService.valueByKey(configKey));
    }

    @Operation(summary = "参数详情")
    @GetMapping("/{configId}")
    @RequiresPerm({"system:config:query", "system:config:list"})
    public Result<SysConfig> detail(@PathVariable Long configId) {
        return Result.ok(configService.detail(configId));
    }

    @Operation(summary = "新增参数")
    @PostMapping
    @RequiresPerm({"system:config:add", "system:config:list"})
    @OperLog(title = "参数配置", businessType = 1)
    public Result<Void> add(@RequestBody SysConfig config) {
        configService.add(config);
        return Result.ok("新增成功", null);
    }

    @Operation(summary = "修改参数")
    @PutMapping
    @RequiresPerm({"system:config:edit", "system:config:list"})
    @OperLog(title = "参数配置", businessType = 2)
    public Result<Void> update(@RequestBody SysConfig config) {
        configService.update(config);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "删除参数（逗号分隔批量）")
    @DeleteMapping("/{configIds}")
    @RequiresPerm({"system:config:remove", "system:config:list"})
    @OperLog(title = "参数配置", businessType = 3)
    public Result<Void> remove(@PathVariable String configIds) {
        configService.remove(configIds);
        return Result.ok("删除成功", null);
    }
}
