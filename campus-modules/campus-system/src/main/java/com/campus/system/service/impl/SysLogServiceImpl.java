package com.campus.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.system.domain.entity.SysLoginLog;
import com.campus.system.domain.entity.SysOperLog;
import com.campus.system.mapper.SysLoginLogMapper;
import com.campus.system.mapper.SysLogCustomMapper;
import com.campus.system.mapper.SysOperLogMapper;
import com.campus.system.service.SysLogService;
import com.campus.system.util.QueryTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 日志查询与清理实现。
 *
 * @author campus-mart
 */
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysLogCustomMapper logCustomMapper;

    @Override
    public PageResult<SysOperLog> pageOper(PageQuery pageQuery, String title, String operName,
                                           String status, String businessType, String beginTime, String endTime) {
        String begin = QueryTimeUtils.pick(pageQuery.getParams(), "beginTime", beginTime);
        String end = QueryTimeUtils.pick(pageQuery.getParams(), "endTime", endTime);
        LocalDateTime beginAt = QueryTimeUtils.begin(begin);
        LocalDateTime endAt = QueryTimeUtils.end(end);

        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.<SysOperLog>lambdaQuery()
                .like(StrUtil.isNotBlank(title), SysOperLog::getTitle, title)
                .like(StrUtil.isNotBlank(operName), SysOperLog::getOperName, operName)
                .eq(StrUtil.isNotBlank(status), SysOperLog::getStatus, status)
                .eq(StrUtil.isNotBlank(businessType), SysOperLog::getBusinessType, businessType)
                .ge(beginAt != null, SysOperLog::getOperTime, beginAt)
                .le(endAt != null, SysOperLog::getOperTime, endAt)
                .orderByDesc(SysOperLog::getOperId);
        Page<SysOperLog> page = operLogMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public void removeOper(String operIds) {
        List<Long> ids = parseIds(operIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的日志");
        // sys_oper_log 没有逻辑删除字段，deleteBatchIds 是物理删除
        operLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void cleanOper() {
        // 全表清空走 TRUNCATE，绕过 BlockAttackInnerInterceptor 的“禁止无 where 删除”
        logCustomMapper.truncateOperLog();
    }

    @Override
    public PageResult<SysLoginLog> pageLogin(PageQuery pageQuery, String userName, String ipaddr,
                                             String status, String beginTime, String endTime) {
        String begin = QueryTimeUtils.pick(pageQuery.getParams(), "beginTime", beginTime);
        String end = QueryTimeUtils.pick(pageQuery.getParams(), "endTime", endTime);
        LocalDateTime beginAt = QueryTimeUtils.begin(begin);
        LocalDateTime endAt = QueryTimeUtils.end(end);

        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.<SysLoginLog>lambdaQuery()
                .like(StrUtil.isNotBlank(userName), SysLoginLog::getUsername, userName)
                .like(StrUtil.isNotBlank(ipaddr), SysLoginLog::getIpaddr, ipaddr)
                .eq(StrUtil.isNotBlank(status), SysLoginLog::getStatus, status)
                .ge(beginAt != null, SysLoginLog::getLoginTime, beginAt)
                .le(endAt != null, SysLoginLog::getLoginTime, endAt)
                .orderByDesc(SysLoginLog::getInfoId);
        Page<SysLoginLog> page = loginLogMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public void removeLogin(String infoIds) {
        List<Long> ids = parseIds(infoIds);
        ServiceException.throwIf(ids.isEmpty(), "请选择要删除的日志");
        loginLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void cleanLogin() {
        logCustomMapper.truncateLoginLog();
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
