package com.campus.item.controller;

import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.common.log.OperLog;
import com.campus.common.security.RequiresPerm;
import com.campus.item.dto.OrderQueryDTO;
import com.campus.item.dto.OrderRefundDTO;
import com.campus.item.dto.OrderStatVO;
import com.campus.item.dto.OrderStatusDTO;
import com.campus.item.dto.OrderVO;
import com.campus.item.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-订单管理接口
 *
 * @author campus-mart
 */
@Tag(name = "后台-订单管理", description = "全量订单分页、人工改状态（退款等）、今日订单统计")
@RestController
@RequestMapping("/item/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页", description = "按订单号/商品/买家/卖家/状态/下单时间范围筛选")
    @RequiresPerm("item:order:list")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@Valid OrderQueryDTO query) {
        return Result.ok(orderService.adminPage(query));
    }

    @Operation(summary = "人工修改订单状态", description = "退款等人工处理：按状态机校验（2→4→5 退款流程），同步商品状态并通知买家")
    @RequiresPerm("item:order:refund")
    @OperLog(title = "订单管理", businessType = 2)
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody OrderStatusDTO dto) {
        orderService.adminUpdateStatus(dto);
        return Result.ok("订单状态修改成功", null);
    }

    @Operation(summary = "订单退款", description = "交易中/已完成（1/2）的订单发起退款 → 4 退款中，复用状态机校验")
    @RequiresPerm({"item:order:refund", "item:order:list"})
    @OperLog(title = "订单管理", businessType = 2)
    @PutMapping("/refund")
    public Result<Void> refund(@Valid @RequestBody OrderRefundDTO dto) {
        orderService.adminRefund(dto.getOrderId(), dto.getReason());
        return Result.ok("退款已提交", null);
    }

    @Operation(summary = "今日订单统计", description = "今日订单数、今日成交额、待处理（待确认+待交付）订单数，供后台列表页顶部展示")
    @RequiresPerm("item:order:list")
    @GetMapping("/stat/today")
    public Result<OrderStatVO> todayStat() {
        return Result.ok(orderService.todayStat());
    }
}
