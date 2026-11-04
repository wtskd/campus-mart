package com.campus.ai.mapper;

import com.campus.ai.domain.dto.FavoritePair;
import com.campus.api.dto.ItemBriefDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品只读查询 Mapper（Feign 不可用时的本地兜底）
 *
 * <p>本模块只读 biz_item / biz_favorite，不写业务表；优先走 ItemClient Feign，
 * Feign 失败（比如只启动 campus-ai 单模块演示）才回退到本地查询。</p>
 *
 * @author campus-mart
 */
@Mapper
public interface ItemReadMapper {

    @Select("SELECT i.id AS item_id, i.item_no, i.title, i.description, i.price, "
            + "i.category_id, c.name AS category_name, i.seller_id, i.status, "
            + "i.item_condition, i.cover_url, i.view_count, i.favorite_count "
            + "FROM biz_item i LEFT JOIN biz_category c ON c.id = i.category_id "
            + "WHERE i.status = 2 AND i.del_flag = 0 "
            + "ORDER BY (i.view_count + i.favorite_count * 3) DESC LIMIT #{limit}")
    List<ItemBriefDTO> listOnSale(@Param("limit") int limit);

    @Select("SELECT i.id AS item_id, i.item_no, i.title, i.description, i.price, "
            + "i.category_id, c.name AS category_name, i.seller_id, i.status, "
            + "i.item_condition, i.cover_url, i.view_count, i.favorite_count "
            + "FROM biz_item i LEFT JOIN biz_category c ON c.id = i.category_id "
            + "WHERE i.id = #{itemId} AND i.del_flag = 0")
    ItemBriefDTO getItem(@Param("itemId") Long itemId);

    @Select("SELECT i.id AS item_id, i.item_no, i.title, i.description, i.price, "
            + "i.category_id, c.name AS category_name, i.seller_id, i.status, "
            + "i.item_condition, i.cover_url, i.view_count, i.favorite_count "
            + "FROM biz_favorite f JOIN biz_item i ON i.id = f.item_id "
            + "LEFT JOIN biz_category c ON c.id = i.category_id "
            + "WHERE f.user_id = #{userId} AND i.del_flag = 0 "
            + "ORDER BY f.create_time DESC LIMIT 200")
    List<ItemBriefDTO> listFavorite(@Param("userId") Long userId);

    @Select("SELECT item_id, user_id FROM biz_favorite")
    List<FavoritePair> listAllFavorites();
}
