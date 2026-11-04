package com.campus.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 最新成交动态行
 *
 * @author campus-mart
 */
@Data
public class RecentOrderRow {

    /** 订单号 */
    private String orderNo;

    /** 商品标题 */
    private String itemTitle;

    /** 成交金额 */
    private BigDecimal amount;

    /** 买家昵称 */
    private String buyerNickname;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 成交时间 */
    private LocalDateTime finishTime;
}
