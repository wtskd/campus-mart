package com.campus.item.service;

import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.AdminItemQueryDTO;
import com.campus.item.dto.AppItemQueryDTO;
import com.campus.item.dto.ItemAuditDTO;
import com.campus.item.dto.ItemDetailVO;
import com.campus.item.dto.ItemListVO;
import com.campus.item.dto.ItemPublishDTO;
import com.campus.item.dto.ItemPublishVO;
import com.campus.item.dto.ItemStatusDTO;
import com.campus.item.dto.ItemUpdateDTO;

import java.util.List;

/**
 * 商品服务
 *
 * @author campus-mart
 */
public interface ItemService {

    /**
     * 前台在售商品分页（只查 status=2 且未删除）
     */
    PageResult<ItemListVO> pageAppItems(AppItemQueryDTO query);

    /**
     * 商品详情（浏览量自增、含卖家信息、是否已收藏）
     */
    ItemDetailVO getDetail(Long itemId);

    /**
     * 发布商品（AI 内容审核）
     */
    ItemPublishVO publish(ItemPublishDTO dto);

    /**
     * 编辑自己的商品（改标题/描述后重走 AI 审核）
     */
    ItemPublishVO update(ItemUpdateDTO dto);

    /**
     * 下架自己的在售商品（2 → 5）
     */
    void offShelf(Long itemId);

    /**
     * 我发布的商品分页
     */
    PageResult<ItemListVO> pageMyItems(AdminItemQueryDTO query);

    /**
     * 推荐兜底：热门在售商品
     */
    List<ItemListVO> recommend(Integer limit);

    /**
     * 后台全量商品分页
     */
    PageResult<ItemListVO> pageAdminItems(AdminItemQueryDTO query);

    /**
     * 后台审核：通过 1 → 2 并置上架时间；驳回 1 → 5
     */
    void audit(ItemAuditDTO dto);

    /**
     * 后台强制修改状态
     */
    void forceStatus(ItemStatusDTO dto);

    /**
     * 后台逻辑删除商品
     */
    void adminDelete(Long itemId);

    /**
     * 内部接口：商品概要
     */
    ItemBriefDTO getItemBrief(Long itemId);

    /**
     * 内部接口：在售商品概要列表（AI 推荐候选集）
     */
    List<ItemBriefDTO> listOnSaleBrief(Integer limit);
}
