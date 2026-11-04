package com.campus.item.controller;

import com.campus.common.core.domain.PageQuery;
import com.campus.common.core.domain.PageResult;
import com.campus.common.core.domain.Result;
import com.campus.item.dto.OrderCreateDTO;
import com.campus.item.dto.OrderVO;
import com.campus.item.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 前台-订单接口（需登录）
 *
 * <p>二手单品交易：一个商品同时只能有一条进行中的订单。状态流转：
 * 0待确认 → 1待交付 → 2已完成；0/1 → 3已取消；2 → 4退款中 → 5已退款。</p>
 *
 * @author campus-mart
 */
@Tag(name = "前台-订单", description = "下单、付款、交付、确认收货、取消、我的订单")
@RestController
@RequestMapping("/item/app/order")
@RequiredArgsConstructor
public class AppOrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单", description = "对在售商品下单（不能买自己的商品），成功后商品置为已预订，并给卖家发订单消息")
    @PostMapping("")
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok("下单成功", orderService.createOrder(dto));
    }

    @Operation(summary = "模拟付款", description = "买家付款：0待确认 → 1待交付，写 pay_time；已处于 1 时幂等返回")
    @RequestMapping(value = "/{orderId}/pay", method = {RequestMethod.POST, RequestMethod.PUT})
    public Result<Void> pay(@Parameter(description = "订单ID") @PathVariable("orderId") Long orderId) {
        orderService.pay(orderId);
        return Result.ok("付款成功", null);
    }

    @Operation(summary = "卖家标记交付", description = "卖家确认已当面交付：0 → 1（无独立交付态，幂等）；买家确认收货后完成交易")
    @RequestMapping(value = "/{orderId}/deliver", method = {RequestMethod.POST, RequestMethod.PUT})
    public Result<Void> deliver(@Parameter(description = "订单ID") @PathVariable("orderId") Long orderId) {
        orderService.deliver(orderId);
        return Result.ok("已标记交付，等待买家确认收货", null);
    }

    @Operation(summary = "买家确认收货", description = "1待交付 → 2已完成，写 finish_time，商品置为已售出，卖家信用分 +1")
    @RequestMapping(value = "/{orderId}/confirm", method = {RequestMethod.POST, RequestMethod.PUT})
    public Result<Void> confirm(@Parameter(description = "订单ID") @PathVariable("orderId") Long orderId) {
        orderService.confirm(orderId);
        return Result.ok("确认收货成功", null);
    }

    @Operation(summary = "取消订单", description = "买卖双方均可取消（0待确认/1待交付 → 3已取消），商品恢复在售")
    @RequestMapping(value = "/{orderId}/cancel", method = {RequestMethod.POST, RequestMethod.PUT})
    public Result<Void> cancel(@Parameter(description = "订单ID") @PathVariable("orderId") Long orderId,
                               @Parameter(description = "取消原因") @RequestParam(value = "reason", required = false) String reason,
                               @RequestBody(required = false) java.util.Map<String, Object> body) {
        String why = reason;
        if (why == null && body != null && body.get("cancelReason") != null) {
            why = String.valueOf(body.get("cancelReason"));
        }
        orderService.cancel(orderId, why);
        return Result.ok("订单已取消", null);
    }

    @Operation(summary = "我的订单", description = "role=buy 我买到的 / role=sell 我卖出的，可分页与按状态筛选")
    @GetMapping("/my")
    public Result<PageResult<OrderVO>> my(@Parameter(description = "buy 买到的 / sell 卖出的")
                                          @RequestParam(value = "role", defaultValue = "buy") String role,
                                          @Parameter(description = "订单状态，不传查全部")
                                          @RequestParam(value = "status", required = false) Integer status,
                                          @Valid PageQuery pageQuery) {
        return Result.ok(orderService.myOrders(role, status, pageQuery));
    }

    @Operation(summary = "订单详情", description = "仅订单买家或卖家可查看")
    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@Parameter(description = "订单ID") @PathVariable("orderId") Long orderId) {
        return Result.ok(orderService.detail(orderId));
    }
}
