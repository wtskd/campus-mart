package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * RFM 打分原始行（SQL 窗口函数计算 R/F/M 得分）
 *
 * @author campus-mart
 */
@Data
public class RfmScoredRow {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 院系名称 */
    private String deptName;

    /** 最近一次成交距今天数（无成交为 999） */
    private Integer recencyDays;

    /** 成交次数 */
    private Integer frequency;

    /** 成交金额 */
    private BigDecimal monetary;

    /** R 得分 1-5 */
    private Integer rScore;

    /** F 得分 1-5 */
    private Integer fScore;

    /** M 得分 1-5 */
    private Integer mScore;
}
