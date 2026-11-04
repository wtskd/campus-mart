package com.campus.item.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.item.dto.FavoriteItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * biz_item / biz_favorite 自定义 SQL（原子更新与联表查询，避免 N+1）
 *
 * @author campus-mart
 */
@Mapper
public interface ItemCustomMapper {

    /**
     * 浏览量原子自增
     */
    @Update("UPDATE biz_item SET view_count = view_count + 1 WHERE item_id = #{itemId}")
    int increaseViewCount(@Param("itemId") Long itemId);

    /**
     * 收藏量原子增减，最低为 0
     */
    @Update("UPDATE biz_item SET favorite_count = GREATEST(favorite_count + #{delta}, 0) WHERE item_id = #{itemId}")
    int updateFavoriteCount(@Param("itemId") Long itemId, @Param("delta") int delta);

    /**
     * 统计分类下未删除商品数量（删除分类前校验）
     */
    @Select("SELECT COUNT(*) FROM biz_item WHERE category_id = #{categoryId} AND del_flag = '0'")
    long countByCategory(@Param("categoryId") Long categoryId);

    /**
     * 单个商品概要（联表带分类名、卖家昵称）
     */
    @Select("SELECT i.item_id, i.item_no, i.title, i.description, i.price, i.category_id,"
            + " c.category_name, i.seller_id, COALESCE(u.nickname, '已注销用户') AS seller_name,"
            + " i.status, i.item_condition, i.cover_url, i.images, i.tags, i.view_count, i.favorite_count"
            + " FROM biz_item i"
            + " LEFT JOIN biz_category c ON c.category_id = i.category_id"
            + " LEFT JOIN sys_user u ON u.user_id = i.seller_id"
            + " WHERE i.item_id = #{itemId} AND i.del_flag = '0'")
    ItemBriefDTO selectBriefById(@Param("itemId") Long itemId);

    /**
     * 在售商品概要列表（AI 推荐候选集）
     */
    @Select("SELECT i.item_id, i.item_no, i.title, i.description, i.price, i.category_id,"
            + " c.category_name, i.seller_id, COALESCE(u.nickname, '已注销用户') AS seller_name,"
            + " i.status, i.item_condition, i.cover_url, i.images, i.tags, i.view_count, i.favorite_count"
            + " FROM biz_item i"
            + " LEFT JOIN biz_category c ON c.category_id = i.category_id"
            + " LEFT JOIN sys_user u ON u.user_id = i.seller_id"
            + " WHERE i.status = 2 AND i.del_flag = '0'"
            + " ORDER BY i.view_count DESC, i.create_time DESC"
            + " LIMIT #{limit}")
    List<ItemBriefDTO> selectOnSaleBrief(@Param("limit") int limit);

    /**
     * 用户收藏的商品概要列表
     */
    @Select("SELECT i.item_id, i.item_no, i.title, i.description, i.price, i.category_id,"
            + " c.category_name, i.seller_id, COALESCE(u.nickname, '已注销用户') AS seller_name,"
            + " i.status, i.item_condition, i.cover_url, i.images, i.tags, i.view_count, i.favorite_count"
            + " FROM biz_favorite f"
            + " INNER JOIN biz_item i ON i.item_id = f.item_id AND i.del_flag = '0'"
            + " LEFT JOIN biz_category c ON c.category_id = i.category_id"
            + " LEFT JOIN sys_user u ON u.user_id = i.seller_id"
            + " WHERE f.user_id = #{userId}"
            + " ORDER BY f.create_time DESC"
            + " LIMIT #{limit}")
    List<ItemBriefDTO> selectFavoriteBrief(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 我的收藏分页（联表查商品摘要）
     */
    @Select("SELECT f.favorite_id, f.item_id, i.title, i.cover_url, i.price, i.original_price,"
            + " i.item_condition, i.category_id, c.category_name, i.status, i.publish_time, f.create_time"
            + " FROM biz_favorite f"
            + " INNER JOIN biz_item i ON i.item_id = f.item_id AND i.del_flag = '0'"
            + " LEFT JOIN biz_category c ON c.category_id = i.category_id"
            + " WHERE f.user_id = #{userId}"
            + " ORDER BY f.create_time DESC")
    IPage<FavoriteItemVO> selectFavoritePage(IPage<FavoriteItemVO> page, @Param("userId") Long userId);
}
