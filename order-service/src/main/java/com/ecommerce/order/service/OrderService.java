package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.vo.CreateOrderVO;

/**
 * 订单服务
 */
public interface OrderService {
    
    /**
     * 创建订单
     */
    Order createOrder(CreateOrderVO createOrderVO);

    /**
     * 查询用户订单列表
     */
    Page<Order> getUserOrders(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 根据订单号查询订单
     */
    Order getOrderByOrderNo(String orderNo);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo, Long userId);

    /**
     * 支付订单
     */
    void payOrder(String orderNo, Long userId);
}



