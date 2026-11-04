package com.campus.system.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.system.domain.dto.DictOptionVO;
import com.campus.system.domain.entity.SysDictData;
import com.campus.system.domain.entity.SysDictType;

import java.util.List;

/**
 * 字典服务（字典类型 + 字典数据合并在一起：两者永远成对维护，拆两个服务只会多一层转发）。
 *
 * @author campus-mart
 */
public interface SysDictService {

    /* ---------------- 字典类型 ---------------- */

    PageResult<SysDictType> pageTypes(PageQuery pageQuery, String dictName, String dictType, String status);

    SysDictType getType(Long dictId);

    void addType(SysDictType type);

    void updateType(SysDictType type);

    void removeTypes(String dictIds);

    List<SysDictType> optionTypes();

    /* ---------------- 字典数据 ---------------- */

    PageResult<SysDictData> pageData(PageQuery pageQuery, String dictType, String dictLabel, String status);

    SysDictData getData(Long dictCode);

    void addData(SysDictData data);

    void updateData(SysDictData data);

    void removeData(String dictCodes);

    /** 前台下拉：走 Redis 缓存 dict:{dictType}，TTL 3600s */
    List<DictOptionVO> dataByType(String dictType);
}
