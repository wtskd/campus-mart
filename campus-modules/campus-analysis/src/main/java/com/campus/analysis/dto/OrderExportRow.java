package com.campus.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单导出 CSV 行
 *
 * @author campus-mart
 */
@Data
public class OrderExportRow {

    /** 订单号 */
    private String orderNo;

    /** 商品标题 */
    private String itemTitle;

    /** 买家昵称 */
    private String buyerName;

    /** 卖家昵称 */
    private String sellerName;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态 */
    private Integer status;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 成交时间 */
    private LocalDateTime finishTime;

    /** 交易地点 */
    private String tradePlace;
}
