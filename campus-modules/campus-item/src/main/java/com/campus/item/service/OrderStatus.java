package com.campus.item.service;

/**
 * 订单状态常量与状态机（以 sql/01-schema.sql 列注释、order_status 字典为准）
 *
 * <p>状态值：</p>
 * <ul>
 *   <li>{@link #PENDING_CONFIRM} 0 待确认：买家已下单，等待卖家确认/买家付款</li>
 *   <li>{@link #PENDING_DELIVER} 1 待交付：卖家已确认或买家已付款，等待线下交付</li>
 *   <li>{@link #FINISHED} 2 已完成：买家确认收货，商品置已售出，计入成交额</li>
 *   <li>{@link #CANCELLED} 3 已取消：商品恢复在售</li>
 *   <li>{@link #REFUNDING} 4 退款中</li>
 *   <li>{@link #REFUNDED} 5 已退款</li>
 * </ul>
 *
 * <p>状态流转：0 → 1 → 2；0/1 → 3；2 → 4 → 5。禁止逆向与跨级流转。</p>
 *
 * @author campus-mart
 */
public final class OrderStatus {

    /** 0 待确认 */
    public static final int PENDING_CONFIRM = 0;

    /** 1 待交付 */
    public static final int PENDING_DELIVER = 1;

    /** 2 已完成 */
    public static final int FINISHED = 2;

    /** 3 已取消 */
    public static final int CANCELLED = 3;

    /** 4 退款中 */
    public static final int REFUNDING = 4;

    /** 5 已退款 */
    public static final int REFUNDED = 5;

    private OrderStatus() {
    }

    /** 状态文案，未知状态返回「未知」 */
    public static String textOf(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case PENDING_CONFIRM -> "待确认";
            case PENDING_DELIVER -> "待交付";
            case FINISHED -> "已完成";
            case CANCELLED -> "已取消";
            case REFUNDING -> "退款中";
            case REFUNDED -> "已退款";
            default -> "未知";
        };
    }

    /** 目标状态是否可以从当前状态流转过去 */
    public static boolean canTransit(int from, int to) {
        return switch (from) {
            case PENDING_CONFIRM -> to == PENDING_DELIVER || to == CANCELLED;
            case PENDING_DELIVER -> to == FINISHED || to == CANCELLED || to == REFUNDING;
            case FINISHED -> to == REFUNDING;
            case REFUNDING -> to == REFUNDED;
            default -> false;
        };
    }
}
