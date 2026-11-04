package com.campus.analysis.constant;

/**
 * 订单状态常量（以数据库 biz_order.status 为准）。
 *
 * <p>口径确认：成交订单 = {@link #FINISHED}（status = 2）。</p>
 *
 * @author campus-mart
 */
public final class OrderStatus {

    private OrderStatus() {
    }

    /** 待确认 */
    public static final int PENDING_CONFIRM = 0;

    /** 待交付 / 交易中 */
    public static final int PENDING_DELIVER = 1;

    /** 已完成（成交） */
    public static final int FINISHED = 2;

    /** 已取消 */
    public static final int CANCELLED = 3;

    /** 退款中 */
    public static final int REFUNDING = 4;

    /** 已退款 */
    public static final int REFUNDED = 5;

    /**
     * 订单状态中文描述
     *
     * @param status 状态值
     * @return 中文描述
     */
    public static String text(Integer status) {
        if (status == null) {
            return "";
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
}
