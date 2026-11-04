package com.campus.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 常用操作封装
 *
 * @author campus-mart
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, normalize(value));
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, normalize(value), timeout, unit);
    }

    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, normalize(value), seconds, TimeUnit.SECONDS);
    }

    /**
     * List.of()/stream().toList() 返回的是 final 的不可变集合，Jackson 默认类型信息（NON_FINAL）
     * 不会给容器写入类型标记，导致反序列化回 Object 时报 "expected VALUE_STRING ... type id"。
     * 这里统一换成可序列化且带类型信息的容器。
     */
    private Object normalize(Object value) {
        if (value instanceof List<?> list && !(value instanceof ArrayList)) {
            return new ArrayList<>(list);
        }
        if (value instanceof Set<?> set && !(set instanceof LinkedHashSet)) {
            return new LinkedHashSet<>(set);
        }
        if (value instanceof Map<?, ?> map && !(map instanceof LinkedHashMap)) {
            return new LinkedHashMap<>(map);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            // 旧版本写入的缓存格式不兼容时，按未命中处理并清掉脏键，避免接口 500
            log.warn("缓存反序列化失败，已清除脏键：{}，原因：{}", key, e.getMessage());
            safeDelete(key);
            return null;
        }
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public long delete(Collection<String> keys) {
        Long n = redisTemplate.delete(keys);
        return n == null ? 0 : n;
    }

    public boolean expire(String key, long seconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, seconds, TimeUnit.SECONDS));
    }

    public long getExpire(String key) {
        Long v = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return v == null ? -2 : v;
    }

    /** 自增，返回自增后的值；首次调用时设置过期时间 */
    public long increment(String key, long delta, long expireSeconds) {
        Long v = redisTemplate.opsForValue().increment(key, delta);
        long val = v == null ? 0 : v;
        if (val == delta && expireSeconds > 0) {
            expire(key, expireSeconds);
        }
        return val;
    }

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (RuntimeException e) {
            log.warn("缓存反序列化失败，已清除脏键：{}，原因：{}", key, e.getMessage());
            safeDelete(key);
            return null;
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignore) {
            // ignore
        }
    }
}
