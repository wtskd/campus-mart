package com.campus.item.service;

import com.campus.item.dto.UserBriefVO;

import java.util.Collection;
import java.util.Map;

/**
 * sys_user 用户概要查询（本模块不持有 sys_user 实体，走自定义 SQL + 本地缓存）
 *
 * @author campus-mart
 */
public interface SysUserLookupService {

    /**
     * 查询单个用户概要；用户不存在或已注销时返回「已注销用户」占位对象
     */
    UserBriefVO getUser(Long userId);

    /**
     * 批量查询用户概要，避免列表页 N+1；空集合直接返回空 Map
     */
    Map<Long, UserBriefVO> getUserMap(Collection<Long> userIds);
}
