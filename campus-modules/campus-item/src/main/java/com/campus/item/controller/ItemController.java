package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.AdminItemQueryDTO;
import com.campus.item.dto.ItemAuditDTO;
import com.campus.item.dto.ItemListVO;
import com.campus.item.dto.ItemStatusDTO;
import com.campus.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台商品管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-商品管理", description = "商品审核、强制下架、删除")
@RestController
@RequestMapping("/item/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "商品分页", description = "全量商品分页，可按状态/关键字/分类/卖家筛选")
    @RequiresPerm({"item:item:list", "item:list"})
    @GetMapping("/page")
    public Result<PageResult<ItemListVO>> page(@Valid AdminItemQueryDTO query) {
        return Result.ok(itemService.pageAdminItems(query));
    }

    @Operation(summary = "商品审核", description = "pass=true → 上架（status=2，置上架时间）；pass=false → 驳回下架（status=5，记录审核意见）")
    @RequiresPerm({"item:item:audit", "item:audit:list"})
    @OperLog(title = "商品审核", businessType = 2)
    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody ItemAuditDTO dto) {
        itemService.audit(dto);
        return Result.ok("审核完成", null);
    }

    @Operation(summary = "强制修改商品状态", description = "后台下架/置违规")
    @RequiresPerm({"item:item:edit", "item:item:audit", "item:list"})
    @OperLog(title = "商品状态修改", businessType = 2)
    @PutMapping("/status")
    public Result<Void> forceStatus(@Valid @RequestBody ItemStatusDTO dto) {
        itemService.forceStatus(dto);
        return Result.ok("状态修改成功", null);
    }

    @Operation(summary = "删除商品", description = "逻辑删除（del_flag='0' → '2'），同时置为违规状态")
    @RequiresPerm({"item:item:remove", "item:list"})
    @OperLog(title = "商品删除", businessType = 3)
    @DeleteMapping("/{itemId}")
    public Result<Void> delete(@Parameter(description = "商品ID") @PathVariable("itemId") Long itemId) {
        itemService.adminDelete(itemId);
        return Result.ok("删除成功", null);
    }
}
