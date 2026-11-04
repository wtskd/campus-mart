package com.campus.item.service;

import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.FavoriteItemVO;

import java.util.List;

/**
 * 商品收藏服务
 *
 * @author campus-mart
 */
public interface FavoriteService {

    /**
     * 收藏商品（幂等，重复收藏不报错）
     */
    void add(Long itemId);

    /**
     * 取消收藏（幂等）
     */
    void remove(Long itemId);

    /**
     * 我的收藏分页
     */
    PageResult<FavoriteItemVO> pageMyFavorites(PageQuery query);

    /**
     * 是否已收藏
     */
    boolean check(Long itemId);

    /**
     * 内部接口：用户收藏的商品概要列表（最多 200 条）
     */
    List<ItemBriefDTO> listFavoriteItems(Long userId);
}
