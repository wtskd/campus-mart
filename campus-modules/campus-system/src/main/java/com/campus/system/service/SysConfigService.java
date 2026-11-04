package com.campus.system.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.entity.SysConfig;

/**
 * 参数配置服务。
 *
 * @author campus-mart
 */
public interface SysConfigService {

    PageResult<SysConfig> page(PageQuery pageQuery, String configName, String configKey, String configType);

    SysConfig detail(Long configId);

    void add(SysConfig config);

    void update(SysConfig config);

    void remove(String configIds);

    /** 按 key 取值，走 Redis 缓存 config:{key}，TTL 3600s；不存在返回 null */
    String valueByKey(String configKey);
}
