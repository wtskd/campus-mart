package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.utils.RedisUtils;
import com.campus.item.domain.entity.BizCategory;
import com.campus.item.dto.CategorySaveDTO;
import com.campus.item.dto.CategoryStatusDTO;
import com.campus.item.dto.CategoryTreeVO;
import com.campus.item.dto.CategoryUpdateDTO;
import com.campus.item.mapper.BizCategoryMapper;
import com.campus.item.mapper.ItemCustomMapper;
import com.campus.item.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 商品分类服务实现
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    /** 前台分类树缓存 key */
    private static final String TREE_CACHE_KEY = "item:category:tree";

    /** 前台分类树缓存时长：30 分钟 */
    private static final long TREE_CACHE_SECONDS = 30 * 60L;

    /** 正常状态 */
    private static final String STATUS_NORMAL = "0";

    private final BizCategoryMapper categoryMapper;

    private final ItemCustomMapper itemCustomMapper;

    private final RedisUtils redisUtils;

    @Override
    public List<CategoryTreeVO> appTree() {
        try {
            Object cached = redisUtils.get(TREE_CACHE_KEY);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<CategoryTreeVO> tree = (List<CategoryTreeVO>) list;
                return tree;
            }
        } catch (Exception e) {
            log.warn("读取分类树缓存失败，回源数据库：{}", e.getMessage());
        }
        List<BizCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(BizCategory.class)
                .eq(BizCategory::getStatus, STATUS_NORMAL)
                .orderByAsc(BizCategory::getOrderNum)
                .orderByAsc(BizCategory::getCategoryId));
        List<CategoryTreeVO> tree = buildTree(categories);
        try {
            redisUtils.set(TREE_CACHE_KEY, tree, TREE_CACHE_SECONDS);
        } catch (Exception e) {
            log.warn("写入分类树缓存失败：{}", e.getMessage());
        }
        return tree;
    }

    @Override
    public List<CategoryTreeVO> adminList(boolean tree) {
        List<BizCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(BizCategory.class)
                .orderByAsc(BizCategory::getParentId)
                .orderByAsc(BizCategory::getOrderNum)
                .orderByAsc(BizCategory::getCategoryId));
        if (tree) {
            return buildTree(categories);
        }
        return categories.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CategorySaveDTO dto) {
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        checkParent(parentId, null);
        checkNameDuplicate(parentId, dto.getCategoryName(), null);
        BizCategory category = new BizCategory();
        category.setParentId(parentId);
        category.setCategoryName(dto.getCategoryName().trim());
        category.setIcon(dto.getIcon());
        category.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        category.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_NORMAL);
        categoryMapper.insert(category);
        clearTreeCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CategoryUpdateDTO dto) {
        BizCategory exist = categoryMapper.selectById(dto.getCategoryId());
        if (exist == null) {
            throw new ServiceException("分类不存在");
        }
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (Objects.equals(parentId, dto.getCategoryId())) {
            throw new ServiceException("父分类不能是分类自身");
        }
        checkParent(parentId, dto.getCategoryId());
        checkNameDuplicate(parentId, dto.getCategoryName(), dto.getCategoryId());
        BizCategory update = new BizCategory();
        update.setCategoryId(dto.getCategoryId());
        update.setParentId(parentId);
        update.setCategoryName(dto.getCategoryName().trim());
        update.setIcon(dto.getIcon());
        update.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        if (StringUtils.hasText(dto.getStatus())) {
            update.setStatus(dto.getStatus());
        }
        categoryMapper.updateById(update);
        clearTreeCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long categoryId) {
        BizCategory exist = categoryMapper.selectById(categoryId);
        if (exist == null) {
            throw new ServiceException("分类不存在");
        }
        Long children = categoryMapper.selectCount(Wrappers.lambdaQuery(BizCategory.class)
                .eq(BizCategory::getParentId, categoryId));
        if (children != null && children > 0) {
            throw new ServiceException("该分类下存在子分类，无法删除");
        }
        long itemCount = itemCustomMapper.countByCategory(categoryId);
        if (itemCount > 0) {
            throw new ServiceException("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(categoryId);
        clearTreeCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(CategoryStatusDTO dto) {
        BizCategory exist = categoryMapper.selectById(dto.getCategoryId());
        if (exist == null) {
            throw new ServiceException("分类不存在");
        }
        BizCategory update = new BizCategory();
        update.setCategoryId(dto.getCategoryId());
        update.setStatus(dto.getStatus());
        categoryMapper.updateById(update);
        clearTreeCache();
    }

    /**
     * 校验父分类：0 为一级分类；否则必须存在且不能形成环
     */
    private void checkParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        BizCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("父分类不存在");
        }
        if (selfId == null) {
            return;
        }
        // 防止把父分类挂到自己的子孙节点下
        Long cursor = parent.getParentId();
        int depth = 0;
        while (cursor != null && cursor != 0L && depth < 20) {
            if (Objects.equals(cursor, selfId)) {
                throw new ServiceException("不能将分类移动到自己的子分类下");
            }
            BizCategory ancestor = categoryMapper.selectById(cursor);
            cursor = ancestor == null ? null : ancestor.getParentId();
            depth++;
        }
    }

    /**
     * 同级分类名称不能重复
     */
    private void checkNameDuplicate(Long parentId, String categoryName, Long excludeId) {
        if (!StringUtils.hasText(categoryName)) {
            return;
        }
        Long count = categoryMapper.selectCount(Wrappers.lambdaQuery(BizCategory.class)
                .eq(BizCategory::getParentId, parentId)
                .eq(BizCategory::getCategoryName, categoryName.trim())
                .ne(excludeId != null, BizCategory::getCategoryId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException("同级分类名称已存在");
        }
    }

    /**
     * 平铺列表构建两级分类树
     */
    private List<CategoryTreeVO> buildTree(List<BizCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, CategoryTreeVO> idMap = new LinkedHashMap<>();
        for (BizCategory category : categories) {
            idMap.put(category.getCategoryId(), toVO(category));
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO node : idMap.values()) {
            CategoryTreeVO parent = node.getParentId() == null ? null : idMap.get(node.getParentId());
            if (parent == null || Objects.equals(node.getCategoryId(), node.getParentId())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private CategoryTreeVO toVO(BizCategory category) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setParentId(category.getParentId());
        vo.setCategoryName(category.getCategoryName());
        vo.setIcon(category.getIcon());
        vo.setOrderNum(category.getOrderNum());
        vo.setStatus(category.getStatus());
        return vo;
    }

    private void clearTreeCache() {
        try {
            redisUtils.delete(TREE_CACHE_KEY);
        } catch (Exception e) {
            log.warn("清除分类树缓存失败：{}", e.getMessage());
        }
    }
}
