package com.campus.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.exception.ServiceException;
import com.campus.common.security.SecurityUtils;
import com.campus.common.utils.RedisUtils;
import com.campus.item.domain.entity.BizItem;
import com.campus.item.domain.entity.BizReport;
import com.campus.item.dto.ReportCreateDTO;
import com.campus.item.dto.ReportHandleDTO;
import com.campus.item.dto.ReportQueryDTO;
import com.campus.item.dto.ReportVO;
import com.campus.item.mapper.BizItemMapper;
import com.campus.item.mapper.BizReportMapper;
import com.campus.item.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 举报服务实现
 *
 * <p>当前 biz_report 表结构只支持商品举报（item_id 非空），targetType=comment/user 返回 400。</p>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 举报状态：0 待处理 */
    private static final int STATUS_PENDING = 0;

    /** 举报状态：1 已处理（举报成立） */
    private static final int STATUS_HANDLED = 1;

    /** 举报状态：2 驳回 */
    private static final int STATUS_REJECTED = 2;

    /** 商品状态：6 违规 */
    private static final int ITEM_VIOLATION = 6;

    /** 商品状态：4 已售出 */
    private static final int ITEM_SOLD = 4;

    /** 防重复举报 Redis key 前缀 */
    private static final String REPORT_DUP_KEY = "item:report:dup:";

    /** 防重复举报窗口：24 小时 */
    private static final long REPORT_DUP_EXPIRE_SECONDS = 24 * 60 * 60L;

    private final BizReportMapper reportMapper;
    private final BizItemMapper itemMapper;
    private final RedisUtils redisUtils;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReportCreateDTO dto) {
        Long reporterId = SecurityUtils.requireUserId();
        if (!"item".equals(dto.getTargetType())) {
            throw new ServiceException(400, "暂不支持举报评论或用户，请举报商品");
        }
        BizItem item = itemMapper.selectById(dto.getTargetId());
        if (item == null) {
            throw new ServiceException(404, "举报的商品不存在或已下架");
        }
        // 同一用户同一商品 24 小时内只能举报一次
        String dupKey = REPORT_DUP_KEY + reporterId + ":" + dto.getTargetId();
        long times = redisUtils.increment(dupKey, 1L, REPORT_DUP_EXPIRE_SECONDS);
        if (times > 1) {
            throw new ServiceException(429, "您已举报过该商品，24 小时内请勿重复举报");
        }
        BizReport report = new BizReport();
        report.setItemId(dto.getTargetId());
        report.setReporterId(reporterId);
        report.setReasonType(dto.getReason());
        report.setContent(dto.getDescription());
        report.setStatus(STATUS_PENDING);
        reportMapper.insert(report);
        return report.getReportId();
    }

    @Override
    public PageResult<ReportVO> adminPage(ReportQueryDTO query) {
        LambdaQueryWrapper<BizReport> wrapper = new LambdaQueryWrapper<>();
        if (query.getItemId() != null) {
            wrapper.eq(BizReport::getItemId, query.getItemId());
        }
        if (query.getReporterId() != null) {
            wrapper.eq(BizReport::getReporterId, query.getReporterId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BizReport::getStatus, query.getStatus());
        }
        if (query.getBeginTime() != null) {
            wrapper.ge(BizReport::getCreateTime, query.getBeginTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(BizReport::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(BizReport::getCreateTime);
        Page<BizReport> page = reportMapper.selectPage(query.toPage(), wrapper);
        PageResult<ReportVO> result = new PageResult<>();
        result.setRecords(toVOList(page.getRecords()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(ReportHandleDTO dto) {
        Long handlerId = SecurityUtils.requireUserId();
        BizReport report = reportMapper.selectById(dto.getReportId());
        if (report == null) {
            throw new ServiceException(404, "举报记录不存在");
        }
        if (report.getStatus() == null || report.getStatus() != STATUS_PENDING) {
            throw new ServiceException(409, "该举报已处理，请刷新");
        }
        int targetStatus = Integer.parseInt(dto.getStatus());
        int rows = reportMapper.update(null, new LambdaUpdateWrapper<BizReport>()
                .eq(BizReport::getReportId, dto.getReportId())
                .eq(BizReport::getStatus, STATUS_PENDING)
                .set(BizReport::getStatus, targetStatus)
                .set(BizReport::getHandleUserId, handlerId)
                .set(BizReport::getHandleRemark, dto.getHandleRemark())
                .set(BizReport::getHandleTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new ServiceException(409, "该举报已处理，请刷新");
        }
        // 举报成立：商品置违规（已售出商品不再改状态）
        if (targetStatus == STATUS_HANDLED && report.getItemId() != null) {
            itemMapper.update(null, new LambdaUpdateWrapper<BizItem>()
                    .eq(BizItem::getItemId, report.getItemId())
                    .ne(BizItem::getStatus, ITEM_SOLD)
                    .set(BizItem::getStatus, ITEM_VIOLATION)
                    .set(BizItem::getUpdateTime, LocalDateTime.now()));
        }
    }

    // ------------------------------------------------------------------ 内部方法

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待处理";
            case STATUS_HANDLED -> "已处理";
            case STATUS_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private List<ReportVO> toVOList(List<BizReport> reports) {
        if (CollectionUtils.isEmpty(reports)) {
            return new ArrayList<>();
        }
        Map<Long, BizItem> items = loadItems(reports.stream()
                .map(BizReport::getItemId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> reporterIds = new HashSet<>();
        for (BizReport report : reports) {
            if (report.getReporterId() != null) {
                reporterIds.add(report.getReporterId());
            }
        }
        Map<Long, String> nicknames = loadNicknames(reporterIds);
        List<ReportVO> list = new ArrayList<>(reports.size());
        for (BizReport report : reports) {
            ReportVO vo = new ReportVO();
            BeanUtils.copyProperties(report, vo);
            BizItem item = items.get(report.getItemId());
            if (item != null) {
                vo.setItemTitle(item.getTitle());
                vo.setItemCover(item.getCoverUrl());
            }
            vo.setReporterNickname(nicknames.get(report.getReporterId()));
            vo.setStatusText(statusText(report.getStatus()));
            list.add(vo);
        }
        return list;
    }

    private Map<Long, BizItem> loadItems(Collection<Long> itemIds) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }
        List<BizItem> items = itemMapper.selectBatchIds(itemIds);
        Map<Long, BizItem> map = new HashMap<>();
        for (BizItem item : items) {
            map.put(item.getItemId(), item);
        }
        return map;
    }

    private Map<Long, String> loadNicknames(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT user_id, COALESCE(NULLIF(nickname, ''), username) AS nickname "
                + "FROM sys_user WHERE user_id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ids.toArray());
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("user_id");
            Object nickname = row.get("nickname");
            if (id != null) {
                map.put(((Number) id).longValue(), nickname == null ? null : nickname.toString());
            }
        }
        return map;
    }
}
