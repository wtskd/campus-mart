package com.campus.analysis.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 卖家排行行
 *
 * @author campus-mart
 */
@Data
public class SellerRankRow {

    /** 卖家用户 ID */
    private Long sellerId;

    /** 卖家昵称 */
    private String nickname;

    /** 院系名称 */
    private String deptName;

    /** 成交金额 */
    private BigDecimal amount;

    /** 成交单数 */
    private Long orderCount;

    /** 收到评价数 */
    private Long commentCount;

    /** 好评数（score >= 4） */
    private Long goodCount;
}
