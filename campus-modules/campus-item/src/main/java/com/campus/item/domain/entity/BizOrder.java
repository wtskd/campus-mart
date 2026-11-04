package com.campus.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * biz_order 实体
 *
 * @author campus-mart
 */
@Data
@TableName("biz_order")
public class BizOrder {

    /**  */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 商品ID（二手单品，一单一物） */
    private Long itemId;

    /**  */
    private Long buyerId;

    /**  */
    private Long sellerId;

    /** 成交金额 */
    private BigDecimal amount;

    /** 0待确认 1待交付 2已完成 3已取消 4退款中 5已退款 */
    private Integer status;

    /** 0线下 1模拟在线 */
    private Integer payType;

    /** 交易地点 */
    private String tradePlace;

    /**  */
    private String buyerRemark;

    /**  */
    private String cancelReason;

    /**  */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**  */
    private LocalDateTime payTime;

    /**  */
    private LocalDateTime finishTime;

    /**  */
    private LocalDateTime cancelTime;

    /**  */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
