package com.campus.analysis.constant;

/**
 * 商品状态常量（以数据库 biz_item.status 为准）。
 *
 * @author campus-mart
 */
public final class ItemStatus {

    private ItemStatus() {
    }

    /** 草稿 */
    public static final int DRAFT = 0;

    /** 待审核 */
    public static final int PENDING_AUDIT = 1;

    /** 在售 */
    public static final int ON_SALE = 2;

    /** 已预订 */
    public static final int RESERVED = 3;

    /** 已售出 */
    public static final int SOLD = 4;

    /** 已下架 */
    public static final int OFF_SHELF = 5;

    /** 违规下架 */
    public static final int VIOLATION = 6;

    /**
     * 商品状态中文描述
     *
     * @param status 状态值
     * @return 中文描述
     */
    public static String text(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING_AUDIT -> "待审核";
            case ON_SALE -> "在售";
            case RESERVED -> "已预订";
            case SOLD -> "已售出";
            case OFF_SHELF -> "已下架";
            case VIOLATION -> "违规下架";
            default -> "未知";
        };
    }
}
