package com.campus.item.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.api.dto.ModerationDTO;
import com.campus.api.dto.ModerationRequest;
import com.campus.api.feign.AiClient;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.LoginUser;
import com.campus.common.security.SecurityUtils;
import com.campus.item.domain.entity.BizCategory;
import com.campus.item.domain.entity.BizFavorite;
import com.campus.item.domain.entity.BizItem;
import com.campus.item.dto.AdminItemQueryDTO;
import com.campus.item.dto.AppItemQueryDTO;
import com.campus.item.dto.ItemAuditDTO;
import com.campus.item.dto.ItemDetailVO;
import com.campus.item.dto.ItemListVO;
import com.campus.item.dto.ItemPublishDTO;
import com.campus.item.dto.ItemPublishVO;
import com.campus.item.dto.ItemStatusDTO;
import com.campus.item.dto.ItemUpdateDTO;
import com.campus.item.dto.UserBriefVO;
import com.campus.item.mapper.BizCategoryMapper;
import com.campus.item.mapper.BizFavoriteMapper;
import com.campus.item.mapper.BizItemMapper;
import com.campus.item.mapper.ItemCustomMapper;
import com.campus.item.service.ItemService;
import com.campus.item.service.SysUserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    /** 商品编号前缀 */
    private static final String ITEM_NO_PREFIX = "IT";

    /** 商品编号时间部分格式 */
    private static final DateTimeFormatter ITEM_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 状态：待审核 */
    private static final int STATUS_PENDING = 1;

    /** 状态：在售 */
    private static final int STATUS_ON_SALE = 2;

    /** 状态：已预订 */
    private static final int STATUS_RESERVED = 3;

    /** 状态：已售出 */
    private static final int STATUS_SOLD = 4;

    /** 状态：已下架 */
    private static final int STATUS_OFF_SHELF = 5;

    /** 状态：违规 */
    private static final int STATUS_VIOLATION = 6;

    /** 推荐默认条数 */
    private static final int DEFAULT_RECOMMEND_LIMIT = 8;

    /** 推荐最大条数 */
    private static final int MAX_RECOMMEND_LIMIT = 20;

    /** 内部接口最大条数 */
    private static final int MAX_INNER_LIMIT = 200;

    /** 摘要最大截取长度 */
    private static final int SUMMARY_MAX_LENGTH = 100;

    private final BizItemMapper itemMapper;

    private final BizCategoryMapper categoryMapper;

    private final BizFavoriteMapper favoriteMapper;

    private final ItemCustomMapper itemCustomMapper;

    private final SysUserLookupService sysUserLookupService;

    private final AiClient aiClient;

    // ============================ 前台 ============================

    @Override
    public PageResult<ItemListVO> pageAppItems(AppItemQueryDTO query) {
        LambdaQueryWrapper<BizItem> wrapper = Wrappers.lambdaQuery(BizItem.class)
                .eq(BizItem::getStatus, STATUS_ON_SALE);
        applyKeyword(wrapper, query.getKeyword());
        // 分类过滤：一级分类下通常没有直接挂商品，需要把子分类一起纳入，否则列表恒为空
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = new ArrayList<>();
            categoryIds.add(query.getCategoryId());
            categoryMapper.selectList(Wrappers.lambdaQuery(BizCategory.class)
                            .eq(BizCategory::getParentId, query.getCategoryId()))
                    .forEach(child -> categoryIds.add(child.getCategoryId()));
            wrapper.in(BizItem::getCategoryId, categoryIds);
        }
        wrapper.ge(query.getMinPrice() != null, BizItem::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, BizItem::getPrice, query.getMaxPrice())
                .eq(query.getItemCondition() != null, BizItem::getItemCondition, query.getItemCondition());
        applySort(wrapper, query.getSort());
        Page<BizItem> page = itemMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> categoryNames = categoryNameMap();
        PageResult<ItemListVO> result = PageResult.of(page, item -> toListVO(item, categoryNames, null));
        // 已登录时补收藏标记，前端列表卡片的心形图标才不会永远是空的
        Long uid = SecurityUtils.getUserId();
        if (uid != null && result.getRecords() != null && !result.getRecords().isEmpty()) {
            Set<Long> favIds = favoriteMapper.selectList(Wrappers.lambdaQuery(BizFavorite.class)
                            .eq(BizFavorite::getUserId, uid)
                            .in(BizFavorite::getItemId,
                                    result.getRecords().stream().map(ItemListVO::getItemId).toList()))
                    .stream().map(BizFavorite::getItemId).collect(Collectors.toSet());
            result.getRecords().forEach(v -> v.setFavorited(favIds.contains(v.getItemId())));
        }
        return result;
    }

    @Override
    public ItemDetailVO getDetail(Long itemId) {
        if (itemId == null) {
            throw new ServiceException("商品ID不能为空");
        }
        BizItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new ServiceException("商品不存在或已下架");
        }
        Long userId = SecurityUtils.getUserId();
        LoginUser loginUser = SecurityUtils.get();
        boolean admin = loginUser != null && loginUser.isAdmin();
        boolean owner = userId != null && userId.equals(item.getSellerId());
        boolean visible = isPubliclyVisible(item.getStatus());
        if (!visible && !owner && !admin) {
            throw new ServiceException("商品不存在或已下架");
        }

        // 浏览量原子自增（update set view_count = view_count + 1），避免并发覆盖
        if (visible) {
            itemCustomMapper.increaseViewCount(itemId);
            item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        }

        ItemDetailVO vo = new ItemDetailVO();
        vo.setItemId(item.getItemId());
        vo.setItemNo(item.getItemNo());
        vo.setSellerId(item.getSellerId());
        vo.setCategoryId(item.getCategoryId());
        vo.setTitle(item.getTitle());
        vo.setSummary(item.getSummary());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setItemCondition(item.getItemCondition());
        vo.setTradeType(item.getTradeType());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setImages(parseJsonList(item.getImages()));
        vo.setTags(parseJsonList(item.getTags()));
        vo.setLocation(item.getLocation());
        vo.setContactType(item.getContactType());
        vo.setAiGenerated(item.getAiGenerated());
        vo.setViewCount(item.getViewCount());
        vo.setFavoriteCount(item.getFavoriteCount());
        vo.setStatus(item.getStatus());
        vo.setAuditRemark(item.getAuditRemark());
        vo.setPublishTime(item.getPublishTime() != null ? item.getPublishTime() : item.getCreateTime());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());

        BizCategory category = item.getCategoryId() == null ? null : categoryMapper.selectById(item.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getCategoryName());
        vo.setSeller(sysUserLookupService.getUser(item.getSellerId()));

        boolean favorited = false;
        if (userId != null) {
            Long count = favoriteMapper.selectCount(Wrappers.lambdaQuery(BizFavorite.class)
                    .eq(BizFavorite::getUserId, userId)
                    .eq(BizFavorite::getItemId, itemId));
            favorited = count != null && count > 0;
        }
        vo.setFavorited(favorited);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemPublishVO publish(ItemPublishDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        checkCategory(dto.getCategoryId());

        // AI 内容审核：调用失败（AI 服务不可用）时兜底放行
        ModerationDTO moderation = moderate(dto.getTitle(), dto.getDescription(), "item", null);
        boolean pass = moderation == null || moderation.isPass();

        BizItem item = new BizItem();
        item.setItemNo(generateItemNo());
        item.setSellerId(userId);
        item.setCategoryId(dto.getCategoryId());
        item.setTitle(dto.getTitle().trim());
        item.setSummary(buildSummary(dto.getSummary(), dto.getDescription()));
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setOriginalPrice(dto.getOriginalPrice());
        item.setItemCondition(dto.getItemCondition());
        item.setTradeType(dto.getTradeType() == null ? 1 : dto.getTradeType());
        item.setCoverUrl(resolveCoverUrl(dto.getCoverUrl(), dto.getImages()));
        item.setImages(toJsonString(dto.getImages()));
        item.setTags(toJsonString(dto.getTags()));
        item.setLocation(dto.getLocation());
        item.setContactType(dto.getContactType() == null ? 0 : dto.getContactType());
        item.setAiGenerated("0");
        item.setViewCount(0);
        item.setFavoriteCount(0);
        item.setStatus(pass ? STATUS_PENDING : STATUS_VIOLATION);
        item.setAuditRemark(pass ? "" : buildModerationRemark(moderation));
        itemMapper.insert(item);

        ItemPublishVO vo = new ItemPublishVO();
        vo.setItemId(item.getItemId());
        vo.setItemNo(item.getItemNo());
        vo.setStatus(item.getStatus());
        vo.setAuditPass(pass);
        vo.setAuditRemark(item.getAuditRemark());
        vo.setMessage(pass ? "发布成功，等待平台审核" : "商品内容未通过审核：" + item.getAuditRemark());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemPublishVO update(ItemUpdateDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        BizItem item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new ServiceException("商品不存在");
        }
        if (!userId.equals(item.getSellerId())) {
            throw new ServiceException(403, "无权操作他人的商品");
        }
        Integer status = item.getStatus();
        if (status == null || (status != STATUS_PENDING && status != STATUS_ON_SALE && status != STATUS_OFF_SHELF)) {
            throw new ServiceException("当前商品状态不允许编辑");
        }
        checkCategory(dto.getCategoryId());

        boolean contentChanged = !Objects.equals(item.getTitle(), dto.getTitle())
                || !Objects.equals(item.getDescription(), dto.getDescription());
        ModerationDTO moderation = null;
        boolean pass = true;
        if (contentChanged) {
            moderation = moderate(dto.getTitle(), dto.getDescription(), "item", item.getItemId());
            pass = moderation == null || moderation.isPass();
        }

        item.setTitle(dto.getTitle().trim());
        item.setSummary(buildSummary(dto.getSummary(), dto.getDescription()));
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setOriginalPrice(dto.getOriginalPrice());
        item.setCategoryId(dto.getCategoryId());
        item.setItemCondition(dto.getItemCondition());
        item.setTradeType(dto.getTradeType() == null ? 1 : dto.getTradeType());
        item.setCoverUrl(resolveCoverUrl(dto.getCoverUrl(), dto.getImages()));
        item.setImages(toJsonString(dto.getImages()));
        item.setTags(toJsonString(dto.getTags()));
        item.setLocation(dto.getLocation());
        item.setContactType(dto.getContactType() == null ? 0 : dto.getContactType());
        if (!pass) {
            item.setStatus(STATUS_VIOLATION);
            item.setAuditRemark(buildModerationRemark(moderation));
        } else if (contentChanged) {
            // 标题/描述变更后重新进入待审核
            item.setStatus(STATUS_PENDING);
            item.setAuditRemark("");
        }
        itemMapper.updateById(item);

        ItemPublishVO vo = new ItemPublishVO();
        vo.setItemId(item.getItemId());
        vo.setItemNo(item.getItemNo());
        vo.setStatus(item.getStatus());
        vo.setAuditPass(pass);
        vo.setAuditRemark(item.getAuditRemark());
        vo.setMessage(pass
                ? (contentChanged ? "修改成功，等待平台重新审核" : "修改成功")
                : "商品内容未通过审核：" + item.getAuditRemark());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long itemId) {
        Long userId = SecurityUtils.requireUserId();
        BizItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new ServiceException("商品不存在");
        }
        if (!userId.equals(item.getSellerId())) {
            throw new ServiceException(403, "无权操作他人的商品");
        }
        if (item.getStatus() == null || item.getStatus() != STATUS_ON_SALE) {
            throw new ServiceException("只有在售商品可以下架");
        }
        BizItem update = new BizItem();
        update.setItemId(itemId);
        update.setStatus(STATUS_OFF_SHELF);
        itemMapper.updateById(update);
    }

    @Override
    public PageResult<ItemListVO> pageMyItems(AdminItemQueryDTO query) {
        Long userId = SecurityUtils.requireUserId();
        LambdaQueryWrapper<BizItem> wrapper = Wrappers.lambdaQuery(BizItem.class)
                .eq(BizItem::getSellerId, userId)
                .eq(query.getStatus() != null, BizItem::getStatus, query.getStatus())
                .eq(query.getCategoryId() != null, BizItem::getCategoryId, query.getCategoryId());
        applyKeyword(wrapper, query.getKeyword());
        wrapper.orderByDesc(BizItem::getCreateTime).orderByDesc(BizItem::getItemId);
        Page<BizItem> page = itemMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> categoryNames = categoryNameMap();
        return PageResult.of(page, item -> toListVO(item, categoryNames, null));
    }

    @Override
    public List<ItemListVO> recommend(Integer limit) {
        int size = (limit == null || limit < 1) ? DEFAULT_RECOMMEND_LIMIT : Math.min(limit, MAX_RECOMMEND_LIMIT);
        LambdaQueryWrapper<BizItem> wrapper = Wrappers.lambdaQuery(BizItem.class)
                .eq(BizItem::getStatus, STATUS_ON_SALE)
                .orderByDesc(BizItem::getViewCount)
                .orderByDesc(BizItem::getCreateTime)
                .orderByDesc(BizItem::getItemId);
        Page<BizItem> page = itemMapper.selectPage(new Page<>(1, size), wrapper);
        Map<Long, String> categoryNames = categoryNameMap();
        return page.getRecords().stream().map(item -> toListVO(item, categoryNames, null)).toList();
    }

    // ============================ 后台 ============================

    @Override
    public PageResult<ItemListVO> pageAdminItems(AdminItemQueryDTO query) {
        LambdaQueryWrapper<BizItem> wrapper = Wrappers.lambdaQuery(BizItem.class)
                .eq(query.getStatus() != null, BizItem::getStatus, query.getStatus())
                .eq(query.getCategoryId() != null, BizItem::getCategoryId, query.getCategoryId())
                .eq(query.getSellerId() != null, BizItem::getSellerId, query.getSellerId());
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(BizItem::getTitle, keyword)
                    .or().like(BizItem::getDescription, keyword)
                    .or().like(BizItem::getItemNo, keyword));
        }
        wrapper.orderByDesc(BizItem::getCreateTime).orderByDesc(BizItem::getItemId);
        Page<BizItem> page = itemMapper.selectPage(query.toPage(), wrapper);
        Map<Long, String> categoryNames = categoryNameMap();
        Map<Long, UserBriefVO> sellerMap = sellerMap(page.getRecords());
        return PageResult.of(page, item -> toListVO(item, categoryNames, sellerMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(ItemAuditDTO dto) {
        BizItem item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new ServiceException("商品不存在");
        }
        if (item.getStatus() == null || item.getStatus() != STATUS_PENDING) {
            throw new ServiceException("只有待审核状态的商品可以审核");
        }
        BizItem update = new BizItem();
        update.setItemId(dto.getItemId());
        if (Boolean.TRUE.equals(dto.getPass())) {
            update.setStatus(STATUS_ON_SALE);
            update.setPublishTime(LocalDateTime.now());
            update.setAuditRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : "");
        } else {
            // 驳回：1 → 5（下架），由卖家修改后重新提交，符合「先审核后上架」流程
            update.setStatus(STATUS_OFF_SHELF);
            update.setAuditRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : "商品信息不符合平台规范，请修改后重新提交");
        }
        itemMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceStatus(ItemStatusDTO dto) {
        BizItem item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new ServiceException("商品不存在");
        }
        BizItem update = new BizItem();
        update.setItemId(dto.getItemId());
        update.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getRemark())) {
            update.setAuditRemark(dto.getRemark().trim());
        } else if (dto.getStatus() == STATUS_VIOLATION) {
            update.setAuditRemark("违规下架");
        }
        if (dto.getStatus() == STATUS_ON_SALE && item.getPublishTime() == null) {
            update.setPublishTime(LocalDateTime.now());
        }
        itemMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Long itemId) {
        BizItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new ServiceException("商品不存在");
        }
        BizItem update = new BizItem();
        update.setItemId(itemId);
        update.setStatus(STATUS_VIOLATION);
        update.setAuditRemark(StringUtils.hasText(item.getAuditRemark()) ? item.getAuditRemark() : "后台删除");
        itemMapper.updateById(update);
        // @TableLogic 逻辑删除：del_flag '0' → '2'
        itemMapper.deleteById(itemId);
    }

    // ============================ 内部接口 ============================

    @Override
    public ItemBriefDTO getItemBrief(Long itemId) {
        if (itemId == null) {
            throw new ServiceException("商品ID不能为空");
        }
        ItemBriefDTO brief = itemCustomMapper.selectBriefById(itemId);
        if (brief == null) {
            throw new ServiceException("商品不存在");
        }
        return brief;
    }

    @Override
    public List<ItemBriefDTO> listOnSaleBrief(Integer limit) {
        int size = (limit == null || limit < 1) ? MAX_INNER_LIMIT : Math.min(limit, MAX_INNER_LIMIT);
        return itemCustomMapper.selectOnSaleBrief(size);
    }

    // ============================ 私有工具 ============================

    /**
     * AI 内容审核：返回 null 表示 AI 服务不可用（调用方按放行处理）
     */
    private ModerationDTO moderate(String title, String description, String scene, Long itemId) {
        try {
            ModerationRequest request = new ModerationRequest();
            request.setItemId(itemId);
            request.setTitle(title);
            request.setDescription(description);
            request.setScene(scene);
            Result<ModerationDTO> result = aiClient.moderate(request);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            log.warn("AI 内容审核返回异常结果，按放行处理：{}", result == null ? "null" : result.getMsg());
            return null;
        } catch (Exception e) {
            log.warn("AI 内容审核调用失败，按放行处理：{}", e.getMessage());
            return null;
        }
    }

    private String buildModerationRemark(ModerationDTO moderation) {
        if (moderation == null) {
            return "内容不符合平台规范";
        }
        if (moderation.getWords() != null && !moderation.getWords().isEmpty()) {
            return "命中敏感词：" + String.join("、", moderation.getWords());
        }
        return StringUtils.hasText(moderation.getReason()) ? moderation.getReason() : "内容不符合平台规范";
    }

    private void checkCategory(Long categoryId) {
        BizCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("商品分类不存在");
        }
        if (!"0".equals(category.getStatus())) {
            throw new ServiceException("该商品分类已停用，请重新选择");
        }
    }

    private boolean isPubliclyVisible(Integer status) {
        return status != null
                && (status == STATUS_ON_SALE || status == STATUS_RESERVED || status == STATUS_SOLD);
    }

    private String generateItemNo() {
        return ITEM_NO_PREFIX + LocalDateTime.now().format(ITEM_NO_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String buildSummary(String summary, String description) {
        if (StringUtils.hasText(summary)) {
            return summary.trim();
        }
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String plain = description.trim();
        return plain.length() <= SUMMARY_MAX_LENGTH ? plain : plain.substring(0, SUMMARY_MAX_LENGTH);
    }

    private String resolveCoverUrl(String coverUrl, List<String> images) {
        if (StringUtils.hasText(coverUrl)) {
            return coverUrl.trim();
        }
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return null;
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(list);
    }

    private List<String> parseJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JSONUtil.toList(json, String.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("解析 JSON 数组失败，按空列表处理：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void applyKeyword(LambdaQueryWrapper<BizItem> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String value = keyword.trim();
        wrapper.and(w -> w.like(BizItem::getTitle, value).or().like(BizItem::getDescription, value));
    }

    private void applySort(LambdaQueryWrapper<BizItem> wrapper, String sort) {
        String value = StringUtils.hasText(sort) ? sort : "new";
        switch (value) {
            case "priceAsc" -> wrapper.orderByAsc(BizItem::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(BizItem::getPrice);
            case "hot" -> wrapper.orderByDesc(BizItem::getViewCount);
            default -> wrapper.orderByDesc(BizItem::getCreateTime);
        }
        wrapper.orderByDesc(BizItem::getItemId);
    }

    private Map<Long, String> categoryNameMap() {
        List<BizCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(BizCategory.class));
        Map<Long, String> map = new HashMap<>(categories.size());
        for (BizCategory category : categories) {
            map.put(category.getCategoryId(), category.getCategoryName());
        }
        return map;
    }

    private Map<Long, UserBriefVO> sellerMap(Collection<BizItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> sellerIds = items.stream().map(BizItem::getSellerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return sysUserLookupService.getUserMap(sellerIds);
    }

    /**
     * 实体 → 列表 VO
     *
     * @param categoryNames 分类ID → 名称
     * @param sellerMap     卖家ID → 概要，可为 null（前台列表不返回卖家昵称）
     */
    private ItemListVO toListVO(BizItem item, Map<Long, String> categoryNames, Map<Long, UserBriefVO> sellerMap) {
        ItemListVO vo = new ItemListVO();
        vo.setItemId(item.getItemId());
        vo.setItemNo(item.getItemNo());
        vo.setTitle(item.getTitle());
        vo.setSummary(item.getSummary());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setItemCondition(item.getItemCondition());
        vo.setTradeType(item.getTradeType());
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryNames == null ? null : categoryNames.get(item.getCategoryId()));
        vo.setSellerId(item.getSellerId());
        if (sellerMap != null && item.getSellerId() != null) {
            UserBriefVO seller = sellerMap.get(item.getSellerId());
            vo.setSellerNickname(seller == null ? null : seller.getNickname());
        }
        vo.setLocation(item.getLocation());
        vo.setStatus(item.getStatus());
        vo.setAuditRemark(item.getAuditRemark());
        vo.setViewCount(item.getViewCount());
        vo.setFavoriteCount(item.getFavoriteCount());
        vo.setPublishTime(item.getPublishTime() != null ? item.getPublishTime() : item.getCreateTime());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }
}
