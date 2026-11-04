package com.campus.item.service;

/**
 * 消息类型常量（biz_message.msg_type，与 sys_dict_data 字典 msg_type 一致）
 *
 * <ul>
 *   <li>{@link #SYSTEM} 1 系统通知</li>
 *   <li>{@link #ORDER} 2 订单消息</li>
 *   <li>{@link #AUDIT} 3 审核结果</li>
 *   <li>{@link #AI} 4 AI 消息</li>
 * </ul>
 *
 * <p>本模块只负责写入订单类消息（{@link #ORDER}），审核结果由商品审核流程写入。</p>
 *
 * @author campus-mart
 */
public final class MsgType {

    /** 1 系统通知 */
    public static final int SYSTEM = 1;

    /** 2 订单消息 */
    public static final int ORDER = 2;

    /** 3 审核结果 */
    public static final int AUDIT = 3;

    /** 4 AI 消息 */
    public static final int AI = 4;

    private MsgType() {
    }

    /** 消息类型文案，未知类型返回「未知」 */
    public static String textOf(Integer msgType) {
        if (msgType == null) {
            return "未知";
        }
        return switch (msgType) {
            case SYSTEM -> "系统通知";
            case ORDER -> "订单消息";
            case AUDIT -> "审核结果";
            case AI -> "AI 消息";
            default -> "未知";
        };
    }
}
