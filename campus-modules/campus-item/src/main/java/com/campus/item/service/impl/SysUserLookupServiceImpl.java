package com.campus.item.service.impl;

import com.campus.item.dto.UserBriefVO;
import com.campus.item.mapper.SysUserCustomMapper;
import com.campus.item.service.SysUserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户概要查询实现：自定义 SQL 批量查 + ConcurrentHashMap 本地缓存（60 秒 TTL）
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysUserLookupServiceImpl implements SysUserLookupService {

    /** 本地缓存有效期：60 秒 */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    /** 缓存条目上限，超过时清理过期条目 */
    private static final int CACHE_SOFT_LIMIT = 5_000;

    /** 已注销用户占位昵称 */
    private static final String DELETED_NICKNAME = "已注销用户";

    private final SysUserCustomMapper sysUserCustomMapper;

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public UserBriefVO getUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return getUserMap(Collections.singletonList(userId)).get(userId);
    }

    @Override
    public Map<Long, UserBriefVO> getUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        long now = System.currentTimeMillis();
        Map<Long, UserBriefVO> result = new HashMap<>();
        List<Long> missing = new ArrayList<>();
        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId == null) {
                continue;
            }
            CacheEntry entry = cache.get(userId);
            if (entry != null && entry.expireAt() > now) {
                result.put(userId, entry.user());
            } else {
                missing.add(userId);
            }
        }
        if (!missing.isEmpty()) {
            List<UserBriefVO> rows = sysUserCustomMapper.selectBriefByIds(missing);
            Map<Long, UserBriefVO> found = rows == null ? Collections.emptyMap()
                    : rows.stream().filter(u -> u.getUserId() != null)
                    .collect(Collectors.toMap(UserBriefVO::getUserId, Function.identity(), (a, b) -> a));
            for (Long userId : missing) {
                UserBriefVO user = found.get(userId);
                if (user == null) {
                    user = deletedUser(userId);
                } else if (user.getNickname() == null || user.getNickname().isBlank()) {
                    user.setNickname("用户" + userId);
                }
                result.put(userId, user);
                cache.put(userId, new CacheEntry(user, now + CACHE_TTL_MILLIS));
            }
            clearExpiredIfNeeded(now);
        }
        return result;
    }

    private void clearExpiredIfNeeded(long now) {
        if (cache.size() <= CACHE_SOFT_LIMIT) {
            return;
        }
        cache.entrySet().removeIf(e -> e.getValue().expireAt() <= now);
    }

    private UserBriefVO deletedUser(Long userId) {
        UserBriefVO user = new UserBriefVO();
        user.setUserId(userId);
        user.setNickname(DELETED_NICKNAME);
        return user;
    }

    /**
     * 缓存条目
     */
    private record CacheEntry(UserBriefVO user, long expireAt) {
    }
}
