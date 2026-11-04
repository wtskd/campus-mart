package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * RFM 人群汇总行
 *
 * @author campus-mart
 */
@Data
public class RfmSegmentRow {

    /** 人群名称 */
    private String segment;

    /** 用户数 */
    private Long userCount;

    /** 成交金额 */
    private BigDecimal monetary;
}
