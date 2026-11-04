package com.campus.ai.service;

import com.campus.ai.mapper.ItemReadMapper;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.api.feign.ItemClient;
import com.campus.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 商品数据读取：优先 ItemClient（Feign），失败回退本地只读 SQL
 *
 * <p>单模块演示（只启动 campus-ai）时 campus-item 不一定在线，这里做降级保证
 * 推荐/相似/摘要/审核接口仍能返回数据。降级原因只 WARN 一次，避免刷日志。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemDataService {

    private final ItemClient itemClient;

    private final ItemReadMapper itemReadMapper;

    private final AtomicBoolean feignWarned = new AtomicBoolean(false);

    /** 在售商品（推荐候选集） */
    public List<ItemBriefDTO> listOnSale(int limit) {
        try {
            Result<List<ItemBriefDTO>> result = itemClient.listOnSale(limit);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            warnOnce("ItemClient.listOnSale 返回失败：{}", result == null ? "null" : result.getMsg());
        } catch (Exception e) {
            warnOnce("ItemClient 不可用，商品数据回退本地只读查询：{}", e.getMessage());
        }
        try {
            List<ItemBriefDTO> list = itemReadMapper.listOnSale(limit);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("本地在售商品查询失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 商品详情 */
    public ItemBriefDTO getItem(Long itemId) {
        if (itemId == null) {
            return null;
        }
        try {
            Result<ItemBriefDTO> result = itemClient.getItem(itemId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            warnOnce("ItemClient.getItem 返回失败：{}", result == null ? "null" : result.getMsg());
        } catch (Exception e) {
            warnOnce("ItemClient 不可用，商品详情回退本地只读查询：{}", e.getMessage());
        }
        try {
            return itemReadMapper.getItem(itemId);
        } catch (Exception e) {
            log.warn("本地商品详情查询失败：{}", e.getMessage());
            return null;
        }
    }

    /** 用户收藏（推荐画像） */
    public List<ItemBriefDTO> listFavorite(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        try {
            Result<List<ItemBriefDTO>> result = itemClient.listFavorite(userId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            warnOnce("ItemClient.listFavorite 返回失败：{}", result == null ? "null" : result.getMsg());
        } catch (Exception e) {
            warnOnce("ItemClient 不可用，收藏数据回退本地只读查询：{}", e.getMessage());
        }
        try {
            List<ItemBriefDTO> list = itemReadMapper.listFavorite(userId);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("本地收藏查询失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /** 全量收藏关系（ItemCF 共现矩阵，数据量小，内存计算） */
    public List<com.campus.ai.domain.dto.FavoritePair> listAllFavorites() {
        try {
            List<com.campus.ai.domain.dto.FavoritePair> list = itemReadMapper.listAllFavorites();
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("收藏共现数据查询失败，推荐退化为分类/热度策略：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void warnOnce(String format, Object... args) {
        if (feignWarned.compareAndSet(false, true)) {
            log.warn(format, args);
        }
    }
}
