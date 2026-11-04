package com.campus.item.service;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.item.dto.OrderCreateDTO;
import com.campus.item.dto.OrderQueryDTO;
import com.campus.item.dto.OrderStatVO;
import com.campus.item.dto.OrderStatusDTO;
import com.campus.item.dto.OrderVO;

/**
 * 订单服务：二手单品交易（一个商品同时只能有一条进行中的订单，无购物车）
 *
 * @author campus-mart
 */
public interface OrderService {

    /**
     * 创建订单（下单）
     *
     * @return 新订单ID
     */
    Long createOrder(OrderCreateDTO dto);

    /** 买家模拟付款：0 → 1 */
    void pay(Long orderId);

    /** 卖家标记已交付：0/1 → 1 */
    void deliver(Long orderId);

    /** 买家确认收货：1 → 2，商品置已售出 */
    void confirm(Long orderId);

    /** 取消订单：0/1 → 3，商品恢复在售 */
    void cancel(Long orderId, String reason);

    /** 我的订单（买到的 / 卖出的） */
    PageResult<OrderVO> myOrders(String role, Integer status, PageQuery pageQuery);

    /** 订单详情（仅买卖双方可见） */
    OrderVO detail(Long orderId);

    /** 后台订单分页 */
    PageResult<OrderVO> adminPage(OrderQueryDTO query);

    /** 后台人工变更订单状态（退款等） */
    void adminUpdateStatus(OrderStatusDTO dto);

    /** 后台发起退款：交易中/已完成 → 退款中（4） */
    void adminRefund(Long orderId, String reason);

    /** 后台列表页顶部统计 */
    OrderStatVO todayStat();
}
