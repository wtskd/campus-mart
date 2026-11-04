package com.campus.analysis.service.impl;

import com.campus.analysis.constant.AnalysisConstants;
import com.campus.analysis.domain.entity.StatUserRfm;
import com.campus.analysis.dto.RfmDTO;
import com.campus.analysis.dto.RfmScoredRow;
import com.campus.analysis.dto.RfmSegmentDTO;
import com.campus.analysis.dto.RfmSegmentRow;
import com.campus.analysis.dto.RfmSignatureRow;
import com.campus.analysis.dto.RfmUserDTO;
import com.campus.analysis.dto.RfmUserRow;
import com.campus.analysis.mapper.StatCustomMapper;
import com.campus.analysis.service.RfmService;
import com.campus.analysis.util.AnalysisUtils;
import com.campus.common.core.domain.PageResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RFM 用户分层服务实现。
 *
 * <p>分层规则（R/F/M 得分 >= 4 视为「高」）：</p>
 * <ul>
 *   <li>高 R + 高 F + 高 M：重要价值客户；</li>
 *   <li>低 R + 高 F + 高 M：重要保持客户；</li>
 *   <li>高 R + 低 F + 高 M：重要发展客户；</li>
 *   <li>低 R + 低 F + 高 M：重要挽留客户；</li>
 *   <li>高 R + 高 F + 低 M：一般价值客户；</li>
 *   <li>低 R + 高 F + 低 M：一般保持客户；</li>
 *   <li>高 R + 低 F + 低 M：一般发展客户；</li>
 *   <li>低 R + 低 F + 低 M：一般挽留客户；</li>
 *   <li>无成交订单的用户标记为「流失」。</li>
 * </ul>
 *
 * @author campus-mart
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RfmServiceImpl implements RfmService {

    /** 8 类人群展示顺序（固定顺序，方便前端图例稳定） */
    private static final List<String> SEGMENT_ORDER = List.of(
            "重要价值客户", "重要保持客户", "重要发展客户", "重要挽留客户",
            "一般价值客户", "一般保持客户", "一般发展客户", "一般挽留客户");

    /** stat_user_rfm 历史数据保留天数 */
    private static final int RFM_KEEP_DAYS = 30;

    private final StatCustomMapper statMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RfmDTO rfm(int page, int limit) {
        LocalDate calcDate = AnalysisUtils.today();
        RfmSignatureRow signature = ensureComputed(calcDate);
        long totalUsers = signature == null ? 0L : AnalysisUtils.nvl(signature.getUserCount());
        BigDecimal totalMonetary = signature == null ? BigDecimal.ZERO : AnalysisUtils.money(signature.getMonetary());

        RfmDTO dto = new RfmDTO();
        dto.setCalcDate(AnalysisUtils.format(calcDate));
        dto.setTotalUsers(totalUsers);
        dto.setTotalMonetary(AnalysisUtils.money(totalMonetary));
        dto.setSegments(buildSegments(calcDate, totalUsers, totalMonetary));
        dto.setDetail(buildDetail(calcDate, page, limit));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RfmSegmentDTO> segments() {
        LocalDate calcDate = AnalysisUtils.today();
        RfmSignatureRow signature = ensureComputed(calcDate);
        long totalUsers = signature == null ? 0L : AnalysisUtils.nvl(signature.getUserCount());
        BigDecimal totalMonetary = signature == null ? BigDecimal.ZERO : AnalysisUtils.money(signature.getMonetary());
        return buildSegments(calcDate, totalUsers, totalMonetary);
    }

    /**
     * 保证当日 RFM 已计算：当日尚无快照时先重算再返回签名信息。
     *
     * @param calcDate 计算日期
     * @return 当日 RFM 签名信息（人数 / 金额合计），无数据时可能为 null
     */
    private RfmSignatureRow ensureComputed(LocalDate calcDate) {
        RfmSignatureRow signature = statMapper.selectRfmSignature(calcDate);
        long totalUsers = signature == null ? 0L : AnalysisUtils.nvl(signature.getUserCount());
        if (totalUsers <= 0) {
            // 当日尚未计算：先算一次再读
            computeRfm(calcDate);
            signature = statMapper.selectRfmSignature(calcDate);
        }
        return signature;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StatUserRfm> computeRfm(LocalDate calcDate) {
        LocalDate date = calcDate == null ? AnalysisUtils.today() : calcDate;
        List<StatUserRfm> list = new ArrayList<>();

        // 1. 有成交的用户：R / F / M + 分位数得分
        List<RfmScoredRow> scored = statMapper.selectRfmScoredUsers();
        if (scored != null) {
            for (RfmScoredRow row : scored) {
                if (row.getUserId() == null) {
                    continue;
                }
                StatUserRfm entity = new StatUserRfm();
                entity.setUserId(row.getUserId());
                entity.setRecencyDays(row.getRecencyDays() == null ? AnalysisConstants.RFM_MAX_RECENCY : row.getRecencyDays());
                entity.setFrequency(AnalysisUtils.nvl(row.getFrequency()));
                entity.setMonetary(AnalysisUtils.money(row.getMonetary()));
                entity.setRScore(AnalysisUtils.nvl(row.getRScore()));
                entity.setFScore(AnalysisUtils.nvl(row.getFScore()));
                entity.setMScore(AnalysisUtils.nvl(row.getMScore()));
                entity.setSegment(classify(row.getRScore(), row.getFScore(), row.getMScore()));
                entity.setCalcDate(date);
                list.add(entity);
            }
        }

        // 2. 无成交的学生用户：标记「流失」（R=999、F=0、M=0、得分均为 1）
        List<RfmScoredRow> churnUsers = statMapper.selectRfmChurnUsers();
        if (churnUsers != null) {
            for (RfmScoredRow row : churnUsers) {
                if (row.getUserId() == null) {
                    continue;
                }
                StatUserRfm entity = new StatUserRfm();
                entity.setUserId(row.getUserId());
                entity.setRecencyDays(AnalysisConstants.RFM_MAX_RECENCY);
                entity.setFrequency(0);
                entity.setMonetary(BigDecimal.ZERO.setScale(AnalysisConstants.MONEY_SCALE));
                entity.setRScore(1);
                entity.setFScore(1);
                entity.setMScore(1);
                entity.setSegment(AnalysisConstants.SEGMENT_LOST);
                entity.setCalcDate(date);
                list.add(entity);
            }
        }

        // 3. 幂等写入：先删当天，再批量插入；顺带清理 30 天前历史
        statMapper.deleteUserRfm(date);
        statMapper.deleteUserRfmBefore(date.minusDays(RFM_KEEP_DAYS));
        if (!list.isEmpty()) {
            statMapper.insertUserRfmBatch(list);
        }
        log.info("[analysis] RFM 计算完成 {}：写入 {} 行", date, list.size());
        return list;
    }

    /**
     * 构建 8 类人群 + 流失的汇总（固定顺序，无数据补 0）。
     *
     * @param calcDate      计算日期
     * @param totalUsers    总人数
     * @param totalMonetary 总成交金额
     * @return 人群汇总
     */
    private List<RfmSegmentDTO> buildSegments(LocalDate calcDate, long totalUsers, BigDecimal totalMonetary) {
        List<RfmSegmentRow> rows = statMapper.selectRfmSegments(calcDate);
        List<RfmSegmentDTO> result = new ArrayList<>();
        List<String> ordered = new ArrayList<>(SEGMENT_ORDER);
        ordered.add(AnalysisConstants.SEGMENT_LOST);
        for (String segment : ordered) {
            RfmSegmentRow row = findSegment(rows, segment);
            long userCount = row == null ? 0L : AnalysisUtils.nvl(row.getUserCount());
            BigDecimal monetary = row == null ? BigDecimal.ZERO : AnalysisUtils.money(row.getMonetary());
            result.add(toSegmentDTO(segment, userCount, monetary, totalUsers, totalMonetary));
        }
        // 兜底：出现未预期的分层名称时追加到末尾，避免数据丢失
        if (rows != null) {
            for (RfmSegmentRow row : rows) {
                if (row.getSegment() != null && !ordered.contains(row.getSegment())) {
                    result.add(toSegmentDTO(row.getSegment(), AnalysisUtils.nvl(row.getUserCount()),
                            AnalysisUtils.money(row.getMonetary()), totalUsers, totalMonetary));
                }
            }
        }
        return result;
    }

    private RfmSegmentDTO toSegmentDTO(String segment, long userCount, BigDecimal monetary,
                                       long totalUsers, BigDecimal totalMonetary) {
        RfmSegmentDTO dto = new RfmSegmentDTO();
        dto.setSegment(segment);
        dto.setUserCount(userCount);
        dto.setUserRatio(AnalysisUtils.rate(userCount, totalUsers));
        dto.setMonetary(AnalysisUtils.money(monetary));
        dto.setMonetaryRatio(AnalysisUtils.rate(monetary, totalMonetary));
        dto.setAvgMonetary(AnalysisUtils.avgOrderAmount(monetary, userCount));
        return dto;
    }

    private RfmSegmentRow findSegment(List<RfmSegmentRow> rows, String segment) {
        if (rows == null) {
            return null;
        }
        for (RfmSegmentRow row : rows) {
            if (segment.equals(row.getSegment())) {
                return row;
            }
        }
        return null;
    }

    /**
     * 明细分页（分页参数做边界保护，避免 offset 溢出）。
     *
     * @param calcDate 计算日期
     * @param page     页码
     * @param limit    每页条数
     * @return 分页结果
     */
    private PageResult<RfmUserDTO> buildDetail(LocalDate calcDate, int page, int limit) {
        int current = Math.max(page, 1);
        int size = limit <= 0 ? 10 : Math.min(limit, 100);
        long total = statMapper.countRfmRows(calcDate, null);
        List<RfmUserDTO> records = new ArrayList<>();
        if (total > 0) {
            long offset = (long) (current - 1) * size;
            if (offset < total) {
                List<RfmUserRow> rows = statMapper.selectRfmRows(calcDate, null, (int) offset, size);
                if (rows != null) {
                    for (RfmUserRow row : rows) {
                        records.add(toUserDTO(row));
                    }
                }
            }
        }
        PageResult<RfmUserDTO> detail = new PageResult<>();
        detail.setRecords(records);
        detail.setTotal(total);
        detail.setCurrent(current);
        detail.setSize(size);
        detail.setPages(total == 0 ? 0 : (total + size - 1) / size);
        return detail;
    }

    private RfmUserDTO toUserDTO(RfmUserRow row) {
        RfmUserDTO dto = new RfmUserDTO();
        dto.setUserId(row.getUserId());
        dto.setNickname(row.getNickname());
        dto.setDeptName(row.getDeptName());
        dto.setRecencyDays(row.getRecencyDays());
        dto.setFrequency(row.getFrequency());
        dto.setMonetary(AnalysisUtils.money(row.getMonetary()));
        dto.setRScore(row.getRScore());
        dto.setFScore(row.getFScore());
        dto.setMScore(row.getMScore());
        dto.setSegment(row.getSegment());
        return dto;
    }

    /**
     * 按 R/F/M 得分聚合人群。
     *
     * @param rScore R 得分 1-5
     * @param fScore F 得分 1-5
     * @param mScore M 得分 1-5
     * @return 人群名称
     */
    private String classify(Integer rScore, Integer fScore, Integer mScore) {
        boolean highR = AnalysisUtils.nvl(rScore) >= AnalysisConstants.RFM_HIGH_SCORE;
        boolean highF = AnalysisUtils.nvl(fScore) >= AnalysisConstants.RFM_HIGH_SCORE;
        boolean highM = AnalysisUtils.nvl(mScore) >= AnalysisConstants.RFM_HIGH_SCORE;
        if (highM) {
            if (highR && highF) {
                return "重要价值客户";
            }
            if (!highR && highF) {
                return "重要保持客户";
            }
            if (highR) {
                return "重要发展客户";
            }
            return "重要挽留客户";
        }
        if (highR && highF) {
            return "一般价值客户";
        }
        if (!highR && highF) {
            return "一般保持客户";
        }
        if (highR) {
            return "一般发展客户";
        }
        return "一般挽留客户";
    }
}
