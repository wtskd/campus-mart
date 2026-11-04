package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.utils.RedisUtils;
import com.campus.system.domain.entity.SysConfig;
import com.campus.system.mapper.SysConfigMapper;
import com.campus.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 参数配置实现。
 *
 * <p>业务代码（发布审核开关、AI 限流阈值等）只依赖 key 取值的缓存，
 * 所以缓存 key 用 config:{configKey}，改动后立即失效。</p>
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private static final String CONFIG_CACHE_PREFIX = "config:";
    private static final long CONFIG_CACHE_SECONDS = 3600L;

    private final SysConfigMapper configMapper;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<SysConfig> page(PageQuery pageQuery, String configName, String configKey, String configType) {
        LambdaQueryWrapper<SysConfig> wrapper = Wrappers.<SysConfig>lambdaQuery()
                .like(StrUtil.isNotBlank(configName), SysConfig::getConfigName, configName)
                .like(StrUtil.isNotBlank(configKey), SysConfig::getConfigKey, configKey)
                .eq(StrUtil.isNotBlank(configType), SysConfig::getConfigType, configType)
                .orderByAsc(SysConfig::getConfigId);
        Page<SysConfig> page = configMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public SysConfig detail(Long configId) {
        SysConfig config = configMapper.selectById(configId);
        ServiceException.throwIf(config == null, "参数不存在");
        return config;
    }

    @Override
    public void add(SysConfig config) {
        ServiceException.throwIf(StrUtil.isBlank(config.getConfigName()), "参数名称不能为空");
        ServiceException.throwIf(StrUtil.isBlank(config.getConfigKey()), "参数键名不能为空");
        ServiceException.throwIf(StrUtil.isBlank(config.getConfigValue()), "参数键值不能为空");
        ServiceException.throwIf(existsKey(config.getConfigKey(), null), "参数键名已存在");
        config.setConfigType(StrUtil.blankToDefault(config.getConfigType(), "N"));
        configMapper.insert(config);
    }

    @Override
    public void update(SysConfig config) {
        ServiceException.throwIf(config.getConfigId() == null, "参数ID不能为空");
        SysConfig exist = configMapper.selectById(config.getConfigId());
        ServiceException.throwIf(exist == null, "参数不存在");
        if (StrUtil.isNotBlank(config.getConfigKey())) {
            ServiceException.throwIf(existsKey(config.getConfigKey(), config.getConfigId()), "参数键名已存在");
        }
        configMapper.updateById(config);
        // 键名可能改过，新旧两个 key 的缓存都要清
        evictCache(exist.getConfigKey());
        evictCache(config.getConfigKey());
    }

    @Override
    public void remove(String configIds) {
        List<Long> ids = parseIds(configIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的参数");
        for (Long configId : ids) {
            SysConfig config = configMapper.selectById(configId);
            if (config == null) {
                continue;
            }
            configMapper.deleteById(configId);
            evictCache(config.getConfigKey());
        }
    }

    @Override
    public String valueByKey(String configKey) {
        ServiceException.throwIf(StrUtil.isBlank(configKey), "参数键名不能为空");
        String key = CONFIG_CACHE_PREFIX + configKey;
        Object cached = redisUtils.get(key);
        if (cached instanceof String value) {
            return value;
        }
        SysConfig config = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
        if (config == null) {
            return null;
        }
        redisUtils.set(key, config.getConfigValue(), CONFIG_CACHE_SECONDS);
        return config.getConfigValue();
    }

    private boolean existsKey(String configKey, Long excludeConfigId) {
        return configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey)
                .ne(excludeConfigId != null, SysConfig::getConfigId, excludeConfigId)) > 0;
    }

    private void evictCache(String configKey) {
        if (StrUtil.isNotBlank(configKey)) {
            redisUtils.delete(CONFIG_CACHE_PREFIX + configKey);
        }
    }

    private List<Long> parseIds(String ids) {
        if (StrUtil.isBlank(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                result.add(Long.valueOf(part.trim()));
            }
        }
        return result;
    }
}
