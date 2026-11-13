package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizFavorite;
import com.campus.item.domain.entity.BizItem;
import com.campus.item.dto.FavoriteItemVO;
import com.campus.item.mapper.BizFavoriteMapper;
import com.campus.item.mapper.BizItemMapper;
import com.campus.item.mapper.ItemCustomMapper;
import com.campus.item.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 商品收藏服务实现
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    /** 状态：在售 */
    private static final int STATUS_ON_SALE = 2;

    /** 状态：已预订 */
    private static final int STATUS_RESERVED = 3;

    /** 内部接口最多返回条数 */
    private static final int MAX_INNER_LIMIT = 200;

    private final BizFavoriteMapper favoriteMapper;

    private final BizItemMapper itemMapper;

    private final ItemCustomMapper itemCustomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long itemId) {
        Long userId = SecurityUtils.requireUserId();
        BizItem item = itemMapper.selectById(itemId);
        if (item == null || !isFavoriteable(item.getStatus())) {
            throw new ServiceException(404, "商品不存在或已下架");
        }
        Long exist = favoriteMapper.selectCount(Wrappers.lambdaQuery(BizFavorite.class)
                .eq(BizFavorite::getUserId, userId)
                .eq(BizFavorite::getItemId, itemId));
        if (exist != null && exist > 0) {
            // 重复收藏：幂等返回，不重复累加收藏量
            return;
        }
        BizFavorite favorite = new BizFavorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // uk_user_item 唯一索引兜底（并发重复收藏）
            log.debug("重复收藏被唯一索引拦截：userId={}, itemId={}", userId, itemId);
            return;
        }
        itemCustomMapper.updateFavoriteCount(itemId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long itemId) {
        Long userId = SecurityUtils.requireUserId();
        int deleted = favoriteMapper.delete(Wrappers.lambdaQuery(BizFavorite.class)
                .eq(BizFavorite::getUserId, userId)
                .eq(BizFavorite::getItemId, itemId));
        if (deleted > 0) {
            itemCustomMapper.updateFavoriteCount(itemId, -1);
        }
    }

    @Override
    public PageResult<FavoriteItemVO> pageMyFavorites(PageQuery query) {
        Long userId = SecurityUtils.requireUserId();
        Page<FavoriteItemVO> page = query.toPage();
        IPage<FavoriteItemVO> result = itemCustomMapper.selectFavoritePage(page, userId);
        return PageResult.of(result);
    }

    @Override
    public boolean check(Long itemId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null || itemId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(Wrappers.lambdaQuery(BizFavorite.class)
                .eq(BizFavorite::getUserId, userId)
                .eq(BizFavorite::getItemId, itemId));
        return count != null && count > 0;
    }

    @Override
    public List<ItemBriefDTO> listFavoriteItems(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return itemCustomMapper.selectFavoriteBrief(userId, MAX_INNER_LIMIT);
    }

    private boolean isFavoriteable(Integer status) {
        return status != null && (status == STATUS_ON_SALE || status == STATUS_RESERVED);
    }
}
