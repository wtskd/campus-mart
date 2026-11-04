package com.campus.analysis.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * stat_user_rfm 明细行（读取预聚合表并关联用户信息）
 *
 * @author campus-mart
 */
@Data
public class RfmUserRow {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 院系名称 */
    private String deptName;

    /** 最近一次成交距今天数 */
    private Integer recencyDays;

    /** 成交次数 */
    private Integer frequency;

    /** 成交金额 */
    private java.math.BigDecimal monetary;

    /** R 得分 */
    private Integer rScore;

    /** F 得分 */
    private Integer fScore;

    /** M 得分 */
    private Integer mScore;

    /** 用户分层 */
    private String segment;

    /** 统计日期 */
    private LocalDate calcDate;
}
