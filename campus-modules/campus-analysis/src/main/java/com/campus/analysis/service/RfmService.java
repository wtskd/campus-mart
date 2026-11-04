package com.campus.analysis.service;

import com.campus.analysis.domain.entity.StatUserRfm;
import com.campus.analysis.dto.RfmDTO;
import com.campus.analysis.dto.RfmSegmentDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * RFM 用户分层服务。
 *
 * <p>口径：只统计 status = 2 的成交订单；R = 最近一次成交距今天数，F = 成交次数，
 * M = 成交金额；按 NTILE(5) 分位数打 1-5 分（分数越高越好），聚合为 8 类人群；
 * 无成交的普通学生用户标记为「流失」。</p>
 *
 * @author campus-mart
 */
public interface RfmService {

    /**
     * 查询 RFM 分层结果（当日未计算时先计算并写入 stat_user_rfm）。
     *
     * @param page  页码，从 1 开始
     * @param limit 每页条数
     * @return 分层汇总 + 明细分页
     */
    RfmDTO rfm(int page, int limit);

    /**
     * 只返回 RFM 人群聚合（供数据大屏 / 前端首屏使用，不含用户明细分页）。
     *
     * @return 各人群汇总（固定顺序，无数据人群补 0）
     */
    List<RfmSegmentDTO> segments();

    /**
     * 重新计算某日 RFM 并写入 stat_user_rfm（先删后插，幂等，事务内执行）。
     *
     * @param calcDate 计算日期
     * @return 写入的用户分层列表
     */
    List<StatUserRfm> computeRfm(LocalDate calcDate);
}
