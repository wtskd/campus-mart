package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * RFM 数据签名（用于判断 stat_user_rfm 是否过期：与实时数据对比）
 *
 * @author campus-mart
 */
@Data
public class RfmSignatureRow {

    /** 用户数 */
    private Long userCount;

    /** 成交金额合计 */
    private BigDecimal monetary;
}
