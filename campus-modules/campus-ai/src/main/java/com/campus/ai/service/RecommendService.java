package com.campus.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.ai.domain.dto.FavoritePair;
import com.campus.ai.domain.dto.RecommendVO;
import com.campus.ai.domain.entity.AiRecommend;
import com.campus.ai.mapper.AiRecommendMapper;
import com.campus.ai.provider.AiProviderRouter;
import com.campus.ai.provider.AiResponse;
import com.campus.ai.support.MockContentFactory;
import com.campus.api.dto.ItemBriefDTO;
import com.campus.common.security.SecurityUtils;
import com.campus.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 场景三：个性化推荐 + 相似商品
 *
 * <p>算法：ItemCF 协同过滤（收藏共现矩阵）+ 分类画像 + 价格带相似度，纯内存计算；
 * 无收藏（冷启动）按热度 view_count + favorite_count*3 排序。结果写 ai_recommend
 * （scene=home，按天去重）并缓存 Redis key=ai:rec:{userId} TTL 30 分钟。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final String CACHE_PREFIX = "ai:rec:";

    private static final long CACHE_SECONDS = 30 * 60L;

    private static final int CANDIDATE_LIMIT = 300;

    private static final int MAX_LIMIT = 20;

    private static final String RECOMMEND_SCENE = "home";

    private static final double PRICE_BAND = 100D;

    private final ItemDataService itemDataService;

    private final AiRecommendMapper aiRecommendMapper;

    private final RedisUtils redisUtils;

    private final AiCallLogService callLogService;

    /**
     * 个性化推荐
     *
     * @param userId 为空时取当前登录用户
     */
    public List<RecommendVO> recommend(Long userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, MAX_LIMIT);
        Long targetUser = userId == null ? SecurityUtils.getUserId() : userId;
        long start = System.currentTimeMillis();
        if (targetUser == null) {
            // 首页免登录（网关白名单）：没有画像时直接按热度推荐，不落库、不写用户缓存
            return anonymousRecommend(size, start);
        }
        List<RecommendVO> cached = readCache(targetUser, size);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<ItemBriefDTO> onSale = itemDataService.listOnSale(CANDIDATE_LIMIT);
        if (onSale == null || onSale.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, ItemBriefDTO> distinct = new LinkedHashMap<>();
        for (ItemBriefDTO item : onSale) {
            if (item != null && item.getItemId() != null) {
                distinct.putIfAbsent(item.getItemId(), item);
            }
        }
        List<ItemBriefDTO> candidates = distinct.values().stream()
                .filter(item -> !targetUser.equals(item.getSellerId()))
                .collect(Collectors.toList());
        List<ItemBriefDTO> favorites = itemDataService.listFavorite(targetUser);
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        Set<Long> favoriteIds = favorites.stream().map(ItemBriefDTO::getItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<ItemBriefDTO> fresh = candidates.stream()
                .filter(item -> !favoriteIds.contains(item.getItemId()))
                .collect(Collectors.toList());

        String provider;
        List<RecommendVO> result;
        if (favorites.isEmpty() || fresh.isEmpty()) {
            result = heatRecommend(fresh.isEmpty() ? candidates : fresh, size);
            provider = "heat";
        } else {
            result = collaborativeRecommend(fresh, favorites, favoriteIds, size);
            provider = "itemcf";
        }
        result.sort(Comparator.comparingDouble(
                (RecommendVO vo) -> vo.getScore() == null ? 0D : vo.getScore()).reversed());
        if (result.size() > size) {
            result = new ArrayList<>(result.subList(0, size));
        }

        saveCache(targetUser, result);
        persistRecommend(targetUser, result);
        AiResponse logRow = AiResponse.ok(provider, "itemcf-v1",
                "推荐 " + result.size() + " 条", 0, 0, System.currentTimeMillis() - start);
        callLogService.write("recommend", null, "userId=" + targetUser + " limit=" + size,
                logRow, AiProviderRouter.STATUS_SUCCESS, null);
        return result;
    }

    /**
     * 相似商品：同分类 + 价格 ±50%，按收藏数排序
     */
    public List<RecommendVO> similar(Long itemId, Integer limit) {
        int size = limit == null || limit <= 0 ? 6 : Math.min(limit, MAX_LIMIT);
        ItemBriefDTO target = itemDataService.getItem(itemId);
        if (target == null) {
            throw new com.campus.common.core.exception.ServiceException("商品不存在或已下架");
        }
        long start = System.currentTimeMillis();
        List<ItemBriefDTO> onSale = itemDataService.listOnSale(CANDIDATE_LIMIT);
        List<ItemBriefDTO> matched = new ArrayList<>();
        if (onSale != null) {
            for (ItemBriefDTO item : onSale) {
                if (item == null || item.getItemId() == null || item.getItemId().equals(itemId)) {
                    continue;
                }
                if (!Objects.equals(target.getCategoryId(), item.getCategoryId())) {
                    continue;
                }
                if (!priceWithinBand(target.getPrice(), item.getPrice(), 0.5D)) {
                    continue;
                }
                matched.add(item);
            }
        }
        long maxHeat = matched.stream().mapToLong(this::heat).max().orElse(0L);
        List<RecommendVO> result = new ArrayList<>();
        for (ItemBriefDTO item : matched) {
            double score = maxHeat <= 0 ? 50D : 30D + 70D * heat(item) / maxHeat;
            if (target.getPrice() != null && item.getPrice() != null && target.getPrice().doubleValue() > 0) {
                double diff = Math.abs(target.getPrice().doubleValue() - item.getPrice().doubleValue())
                        / target.getPrice().doubleValue();
                score += Math.max(0D, 20D * (1D - Math.min(1D, diff)));
            }
            result.add(toVO(item, round2(Math.min(100D, score)),
                    Objects.equals(target.getCategoryName(), item.getCategoryName())
                            ? "同分类 · 价格相近" : "价格相近"));
        }
        result.sort(Comparator.comparingDouble(
                (RecommendVO vo) -> vo.getScore() == null ? 0D : vo.getScore()).reversed());
        if (result.size() > size) {
            result = new ArrayList<>(result.subList(0, size));
        }
        AiResponse logRow = AiResponse.ok("itemcf", "itemcf-v1",
                "相似商品 " + result.size() + " 条", 0, 0, System.currentTimeMillis() - start);
        callLogService.write("recommend", null, "itemId=" + itemId + " limit=" + size,
                logRow, AiProviderRouter.STATUS_SUCCESS, null);
        return result;
    }

    /* ==================== 推荐算法 ==================== */

    /**
     * 匿名用户（未登录，首页白名单接口）：纯热度推荐，不写 ai_recommend、不写 Redis 用户缓存
     */
    private List<RecommendVO> anonymousRecommend(int size, long start) {
        List<ItemBriefDTO> onSale = itemDataService.listOnSale(CANDIDATE_LIMIT);
        if (onSale == null || onSale.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, ItemBriefDTO> distinct = new LinkedHashMap<>();
        for (ItemBriefDTO item : onSale) {
            if (item != null && item.getItemId() != null) {
                distinct.putIfAbsent(item.getItemId(), item);
            }
        }
        List<RecommendVO> result = heatRecommend(new ArrayList<>(distinct.values()), size);
        AiResponse logRow = AiResponse.ok("heat", "heat-v1",
                "匿名用户热度推荐 " + result.size() + " 条", 0, 0, System.currentTimeMillis() - start);
        callLogService.write("recommend", null, "anonymous limit=" + size,
                logRow, AiProviderRouter.STATUS_SUCCESS, null);
        return result;
    }

    private List<RecommendVO> collaborativeRecommend(List<ItemBriefDTO> candidates,
                                                     List<ItemBriefDTO> favorites,
                                                     Set<Long> favoriteIds, int size) {
        Map<Long, Map<Long, Integer>> coOccurrence = buildCoOccurrence();
        double totalFavorites = favorites.size();
        Map<Long, Double> categoryWeight = new HashMap<>();
        Map<Long, Double> priceBandWeight = new HashMap<>();
        for (ItemBriefDTO favorite : favorites) {
            if (favorite.getCategoryId() != null) {
                categoryWeight.merge(favorite.getCategoryId(), 1D, Double::sum);
            }
            priceBandWeight.merge(priceBand(favorite.getPrice()), 1D, Double::sum);
        }

        Map<Long, Double> scores = new HashMap<>();
        Map<Long, String> reasons = new HashMap<>();
        // 1) ItemCF：收藏共现（同一用户收藏过的商品互相加权）
        for (ItemBriefDTO favorite : favorites) {
            if (favorite.getItemId() == null) {
                continue;
            }
            Map<Long, Integer> neighbors = coOccurrence.get(favorite.getItemId());
            if (neighbors == null) {
                continue;
            }
            for (Map.Entry<Long, Integer> entry : neighbors.entrySet()) {
                if (favoriteIds.contains(entry.getKey())) {
                    continue;
                }
                double add = 1.2D * Math.min(1D, entry.getValue() / 3D);
                scores.merge(entry.getKey(), add, Double::sum);
                reasons.putIfAbsent(entry.getKey(), "收藏过相近商品");
            }
        }
        // 2) 分类画像 + 价格带画像 + 热度微调
        for (ItemBriefDTO candidate : candidates) {
            Long itemId = candidate.getItemId();
            double score = scores.getOrDefault(itemId, 0D);
            String reason = reasons.get(itemId);
            boolean related = score > 0;
            if (candidate.getCategoryId() != null && categoryWeight.containsKey(candidate.getCategoryId())) {
                double weight = categoryWeight.get(candidate.getCategoryId()) / totalFavorites;
                score += 0.8D + 0.6D * weight;
                related = true;
                if (reason == null) {
                    reason = "你收藏过同分类商品";
                }
            }
            if (priceBandWeight.containsKey(priceBand(candidate.getPrice()))) {
                double weight = priceBandWeight.get(priceBand(candidate.getPrice())) / totalFavorites;
                score += 0.4D + 0.4D * weight;
                related = true;
                if (reason == null) {
                    reason = "和你看过的价格区间相近";
                }
            }
            if (!related) {
                continue;
            }
            score += Math.max(0D, Math.min(1D, heat(candidate) / 50D)) * 0.3D;
            scores.put(itemId, score);
            reasons.putIfAbsent(itemId, "同校热门");
        }

        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0D);
        List<RecommendVO> result = new ArrayList<>();
        for (ItemBriefDTO candidate : candidates) {
            Double score = scores.get(candidate.getItemId());
            if (score == null || score <= 0) {
                continue;
            }
            double normalized = maxScore <= 0 ? 50D : 40D + 60D * score / maxScore;
            result.add(toVO(candidate, round2(Math.min(100D, normalized)),
                    reasons.getOrDefault(candidate.getItemId(), "为你推荐")));
        }
        // 3) 兜底补充热度推荐，避免画像过窄导致结果不足
        if (result.size() < size) {
            Set<Long> picked = result.stream().map(RecommendVO::getItemId).collect(Collectors.toSet());
            for (RecommendVO vo : heatRecommend(candidates, size)) {
                if (result.size() >= size) {
                    break;
                }
                if (picked.add(vo.getItemId())) {
                    result.add(vo);
                }
            }
        }
        return result;
    }

    private List<RecommendVO> heatRecommend(List<ItemBriefDTO> candidates, int size) {
        List<ItemBriefDTO> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingLong(this::heat).reversed());
        long maxHeat = sorted.isEmpty() ? 0L : heat(sorted.get(0));
        List<RecommendVO> result = new ArrayList<>();
        for (ItemBriefDTO item : sorted) {
            if (result.size() >= size) {
                break;
            }
            double score = maxHeat <= 0 ? 50D : 30D + 70D * heat(item) / maxHeat;
            result.add(toVO(item, round2(Math.min(100D, score)), "同校热门"));
        }
        return result;
    }

    /**
     * 收藏共现矩阵：同一用户收藏过的商品两两加权
     */
    private Map<Long, Map<Long, Integer>> buildCoOccurrence() {
        Map<Long, Map<Long, Integer>> matrix = new HashMap<>();
        List<FavoritePair> pairs = itemDataService.listAllFavorites();
        Map<Long, List<Long>> userItems = new HashMap<>();
        for (FavoritePair pair : pairs) {
            if (pair == null || pair.getUserId() == null || pair.getItemId() == null) {
                continue;
            }
            userItems.computeIfAbsent(pair.getUserId(), key -> new ArrayList<>()).add(pair.getItemId());
        }
        for (List<Long> items : userItems.values()) {
            int count = items.size();
            if (count < 2) {
                continue;
            }
            for (int i = 0; i < count; i++) {
                Map<Long, Integer> row = matrix.computeIfAbsent(items.get(i), key -> new HashMap<>());
                for (int j = 0; j < count; j++) {
                    if (i != j) {
                        row.merge(items.get(j), 1, Integer::sum);
                    }
                }
            }
        }
        return matrix;
    }

    /* ==================== 落库 / 缓存 ==================== */

    private void persistRecommend(Long userId, List<RecommendVO> result) {
        if (result.isEmpty()) {
            return;
        }
        try {
            LocalDate today = LocalDate.now();
            List<AiRecommend> existing = aiRecommendMapper.selectList(new LambdaQueryWrapper<AiRecommend>()
                    .eq(AiRecommend::getUserId, userId)
                    .eq(AiRecommend::getScene, RECOMMEND_SCENE)
                    .eq(AiRecommend::getBatchDate, today));
            Set<Long> existIds = new HashSet<>();
            for (AiRecommend row : existing) {
                existIds.add(row.getItemId());
            }
            for (RecommendVO vo : result) {
                if (vo.getItemId() == null || !existIds.add(vo.getItemId())) {
                    continue;
                }
                AiRecommend row = new AiRecommend();
                row.setUserId(userId);
                row.setItemId(vo.getItemId());
                row.setScore(BigDecimal.valueOf(vo.getScore() == null ? 0D : vo.getScore()));
                row.setReason(MockContentFactory.abbreviate(vo.getReason(), 60));
                row.setScene(RECOMMEND_SCENE);
                row.setBatchDate(today);
                try {
                    aiRecommendMapper.insert(row);
                } catch (DuplicateKeyException e) {
                    log.debug("推荐记录已存在，跳过：user={}, item={}", userId, vo.getItemId());
                }
            }
        } catch (Exception e) {
            log.warn("写入 ai_recommend 失败（不影响推荐结果）：{}", e.getMessage());
        }
    }

    private List<RecommendVO> readCache(Long userId, int size) {
        try {
            Object cached = redisUtils.get(CACHE_PREFIX + userId);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                List<RecommendVO> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof RecommendVO vo) {
                        result.add(vo);
                    }
                }
                if (result.size() > size) {
                    return new ArrayList<>(result.subList(0, size));
                }
                return result;
            }
        } catch (Exception e) {
            log.debug("读取推荐缓存失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveCache(Long userId, List<RecommendVO> result) {
        if (result.isEmpty()) {
            return;
        }
        try {
            redisUtils.set(CACHE_PREFIX + userId, new ArrayList<>(result), CACHE_SECONDS);
        } catch (Exception e) {
            log.debug("写入推荐缓存失败：{}", e.getMessage());
        }
    }

    /* ==================== 工具方法 ==================== */

    private RecommendVO toVO(ItemBriefDTO item, double score, String reason) {
        RecommendVO vo = new RecommendVO();
        vo.setItemId(item.getItemId());
        vo.setTitle(item.getTitle());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setPrice(item.getPrice());
        vo.setCategoryName(item.getCategoryName());
        vo.setScore(score);
        vo.setReason(MockContentFactory.abbreviate(reason, 20));
        return vo;
    }

    private long heat(ItemBriefDTO item) {
        return nz(item.getViewCount()) + nz(item.getFavoriteCount()) * 3L;
    }

    private static long nz(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private static long priceBand(BigDecimal price) {
        if (price == null) {
            return 0L;
        }
        return Math.round(price.doubleValue() / PRICE_BAND);
    }

    private static boolean priceWithinBand(BigDecimal target, BigDecimal candidate, double ratio) {
        if (target == null || candidate == null) {
            return true;
        }
        double base = target.doubleValue();
        if (base <= 0) {
            return true;
        }
        double value = candidate.doubleValue();
        return value >= base * (1D - ratio) && value <= base * (1D + ratio);
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
