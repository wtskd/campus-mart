package com.campus.item.service;

import com.campus.item.dto.CategorySaveDTO;
import com.campus.item.dto.CategoryStatusDTO;
import com.campus.item.dto.CategoryTreeVO;
import com.campus.item.dto.CategoryUpdateDTO;

import java.util.List;

/**
 * 商品分类服务
 *
 * @author campus-mart
 */
public interface CategoryService {

    /**
     * 前台分类树（仅 status='0'，Redis 缓存 30 分钟）
     */
    List<CategoryTreeVO> appTree();

    /**
     * 后台分类列表
     *
     * @param tree true 返回树结构，false 返回平铺列表
     */
    List<CategoryTreeVO> adminList(boolean tree);

    /**
     * 新增分类
     */
    void add(CategorySaveDTO dto);

    /**
     * 编辑分类
     */
    void update(CategoryUpdateDTO dto);

    /**
     * 删除分类（存在子分类或商品时拒绝）
     */
    void delete(Long categoryId);

    /**
     * 修改分类状态
     */
    void updateStatus(CategoryStatusDTO dto);
}
