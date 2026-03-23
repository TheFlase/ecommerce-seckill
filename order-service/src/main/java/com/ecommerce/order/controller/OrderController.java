package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.vo.CreateOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody CreateOrderVO createOrderVO,
                                     @RequestHeader("userId") Long userId) {
        createOrderVO.setUserId(userId);
        Order order = orderService.createOrder(createOrderVO);
        return Result.success(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/list")
    public Result<Page<Order>> getUserOrders(@RequestHeader("userId") Long userId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Order> page = orderService.getUserOrders(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/detail/{orderNo}")
    public Result<Order> getOrderDetail(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        return Result.success(order);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderNo}")
    public Result<?> cancelOrder(@PathVariable String orderNo,
                                 @RequestHeader("userId") Long userId) {
        orderService.cancelOrder(orderNo, userId);
        return Result.success();
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay/{orderNo}")
    public Result<?> payOrder(@PathVariable String orderNo,
                             @RequestHeader("userId") Long userId) {
        orderService.payOrder(orderNo, userId);
        return Result.success();
    }
}



