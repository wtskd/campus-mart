package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.utils.RedisUtils;
import com.campus.system.domain.dto.DictOptionVO;
import com.campus.system.domain.entity.SysDictData;
import com.campus.system.domain.entity.SysDictType;
import com.campus.system.mapper.SysDictDataMapper;
import com.campus.system.mapper.SysDictTypeMapper;
import com.campus.system.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 字典管理实现。
 *
 * <p>字典下拉是前端（后台 + 小程序）最高频的请求，所以按 dictType 整体缓存 3600s；
 * 字典数据一变就删缓存，不做过期等待，避免运营改完标签后前台还显示旧值。</p>
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    /** 字典下拉缓存 key 前缀：dict:{dictType} */
    private static final String DICT_CACHE_PREFIX = "dict:";
    private static final long DICT_CACHE_SECONDS = 3600L;

    private static final String STATUS_ENABLED = "0";

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final RedisUtils redisUtils;

    /* ---------------- 字典类型 ---------------- */

    @Override
    public PageResult<SysDictType> pageTypes(PageQuery pageQuery, String dictName, String dictType, String status) {
        LambdaQueryWrapper<SysDictType> wrapper = Wrappers.<SysDictType>lambdaQuery()
                .like(StrUtil.isNotBlank(dictName), SysDictType::getDictName, dictName)
                .like(StrUtil.isNotBlank(dictType), SysDictType::getDictType, dictType)
                .eq(StrUtil.isNotBlank(status), SysDictType::getStatus, status)
                .orderByAsc(SysDictType::getDictId);
        Page<SysDictType> page = dictTypeMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public SysDictType getType(Long dictId) {
        SysDictType type = dictTypeMapper.selectById(dictId);
        ServiceException.throwIf(type == null, "字典类型不存在");
        return type;
    }

    @Override
    public void addType(SysDictType type) {
        ServiceException.throwIf(StrUtil.isBlank(type.getDictName()), "字典名称不能为空");
        ServiceException.throwIf(StrUtil.isBlank(type.getDictType()), "字典类型不能为空");
        ServiceException.throwIf(existsDictType(type.getDictType(), null), "字典类型已存在");
        type.setStatus(StrUtil.blankToDefault(type.getStatus(), STATUS_ENABLED));
        dictTypeMapper.insert(type);
    }

    @Override
    public void updateType(SysDictType type) {
        ServiceException.throwIf(type.getDictId() == null, "字典ID不能为空");
        SysDictType exist = dictTypeMapper.selectById(type.getDictId());
        ServiceException.throwIf(exist == null, "字典类型不存在");
        if (StrUtil.isNotBlank(type.getDictType())) {
            ServiceException.throwIf(existsDictType(type.getDictType(), type.getDictId()), "字典类型已存在");
        }
        dictTypeMapper.updateById(type);
        // 类型名/编码可能改了，旧编码的缓存要一起清掉
        evictCache(exist.getDictType());
        evictCache(type.getDictType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTypes(String dictIds) {
        List<Long> ids = parseIds(dictIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的字典类型");
        for (Long dictId : ids) {
            SysDictType type = dictTypeMapper.selectById(dictId);
            if (type == null) {
                continue;
            }
            // 删类型必须连带删数据，否则字典数据成为孤儿；这里选择级联删除而不是拒绝，
            // 因为后台交互上「删字典类型」语义就是整类移除
            dictDataMapper.delete(Wrappers.<SysDictData>lambdaQuery()
                    .eq(SysDictData::getDictType, type.getDictType()));
            dictTypeMapper.deleteById(dictId);
            evictCache(type.getDictType());
        }
    }

    @Override
    public List<SysDictType> optionTypes() {
        return dictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getStatus, STATUS_ENABLED)
                .orderByAsc(SysDictType::getDictId));
    }

    /* ---------------- 字典数据 ---------------- */

    @Override
    public PageResult<SysDictData> pageData(PageQuery pageQuery, String dictType, String dictLabel, String status) {
        LambdaQueryWrapper<SysDictData> wrapper = Wrappers.<SysDictData>lambdaQuery()
                .eq(StrUtil.isNotBlank(dictType), SysDictData::getDictType, dictType)
                .like(StrUtil.isNotBlank(dictLabel), SysDictData::getDictLabel, dictLabel)
                .eq(StrUtil.isNotBlank(status), SysDictData::getStatus, status)
                .orderByAsc(SysDictData::getDictType)
                .orderByAsc(SysDictData::getDictSort);
        Page<SysDictData> page = dictDataMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public SysDictData getData(Long dictCode) {
        SysDictData data = dictDataMapper.selectById(dictCode);
        ServiceException.throwIf(data == null, "字典数据不存在");
        return data;
    }

    @Override
    public void addData(SysDictData data) {
        ServiceException.throwIf(StrUtil.isBlank(data.getDictType()), "字典类型不能为空");
        ServiceException.throwIf(StrUtil.isBlank(data.getDictLabel()), "字典标签不能为空");
        ServiceException.throwIf(StrUtil.isBlank(data.getDictValue()), "字典值不能为空");
        if (data.getDictSort() == null) {
            data.setDictSort(0);
        }
        data.setStatus(StrUtil.blankToDefault(data.getStatus(), STATUS_ENABLED));
        data.setIsDefault(StrUtil.blankToDefault(data.getIsDefault(), "N"));
        dictDataMapper.insert(data);
        evictCache(data.getDictType());
    }

    @Override
    public void updateData(SysDictData data) {
        ServiceException.throwIf(data.getDictCode() == null, "字典数据ID不能为空");
        SysDictData exist = dictDataMapper.selectById(data.getDictCode());
        ServiceException.throwIf(exist == null, "字典数据不存在");
        dictDataMapper.updateById(data);
        // 允许把记录移动到另一个 dictType，两个类型的缓存都要失效
        evictCache(exist.getDictType());
        evictCache(data.getDictType());
    }

    @Override
    public void removeData(String dictCodes) {
        List<Long> ids = parseIds(dictCodes);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的字典数据");
        for (Long dictCode : ids) {
            SysDictData data = dictDataMapper.selectById(dictCode);
            if (data == null) {
                continue;
            }
            dictDataMapper.deleteById(dictCode);
            evictCache(data.getDictType());
        }
    }

    @Override
    public List<DictOptionVO> dataByType(String dictType) {
        ServiceException.throwIf(StrUtil.isBlank(dictType), "字典类型不能为空");
        String key = DICT_CACHE_PREFIX + dictType;
        Object cached = redisUtils.get(key);
        // 缓存里是 JSON 反序列化后的 List<DictOptionVO>，按元素类型过滤，避免脏数据直接抛 ClassCastException
        if (cached instanceof List<?> list && (list.isEmpty() || list.get(0) instanceof DictOptionVO)) {
            return list.stream().filter(DictOptionVO.class::isInstance)
                    .map(DictOptionVO.class::cast).toList();
        }
        List<SysDictData> dataList = dictDataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, STATUS_ENABLED)
                .orderByAsc(SysDictData::getDictSort));
        List<DictOptionVO> options = dataList.stream()
                .map(d -> new DictOptionVO(d.getDictLabel(), d.getDictValue(), d.getCssClass(),
                        d.getIsDefault(), d.getListClass()))
                .toList();
        redisUtils.set(key, options, DICT_CACHE_SECONDS);
        return options;
    }

    /* ---------------- 私有方法 ---------------- */

    private boolean existsDictType(String dictType, Long excludeDictId) {
        return dictTypeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeDictId != null, SysDictType::getDictId, excludeDictId)) > 0;
    }

    private void evictCache(String dictType) {
        if (StrUtil.isNotBlank(dictType)) {
            redisUtils.delete(DICT_CACHE_PREFIX + dictType);
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
