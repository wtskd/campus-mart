package com.campus.api.feign;

import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 campus-item 服务（AI 模块读取商品数据用）
 *
 * @author campus-mart
 */
@FeignClient(name = "campus-item", contextId = "itemClient", path = "/item/inner")
public interface ItemClient {

    /** 商品详情 */
    @GetMapping("/item/{itemId}")
    Result<ItemBriefDTO> getItem(@PathVariable("itemId") Long itemId);

    /** 在售商品列表（推荐候选集） */
    @GetMapping("/item/on-sale")
    Result<List<ItemBriefDTO>> listOnSale(@RequestParam(value = "limit", defaultValue = "200") Integer limit);

    /** 用户收藏过的商品 */
    @GetMapping("/item/favorite/{userId}")
    Result<List<ItemBriefDTO>> listFavorite(@PathVariable("userId") Long userId);
}
