package com.ecommerce.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.feign.ProductFeignClient;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.vo.CreateOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductFeignClient productFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderVO createOrderVO) {
        // 扣减库存
        Result<Boolean> result = productFeignClient.deductStock(
                createOrderVO.getProductId(), 
                createOrderVO.getQuantity()
        );
        
        if (result.getData() == null || !result.getData()) {
            throw new BusinessException("库存不足");
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(createOrderVO.getUserId());
        order.setProductId(createOrderVO.getProductId());
        order.setProductName(createOrderVO.getProductName());
        order.setQuantity(createOrderVO.getQuantity());
        order.setPrice(createOrderVO.getPrice());
        order.setTotalPrice(createOrderVO.getPrice().multiply(
                new java.math.BigDecimal(createOrderVO.getQuantity())
        ));
        order.setStatus(0); // 待支付
        order.setOrderType(0); // 普通订单
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("创建订单成功，订单号：{}", order.getOrderNo());
        return order;
    }

    @Override
    public Page<Order> getUserOrders(Long userId, Integer pageNum, Integer pageSize) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        
        // 验证用户
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 只有待支付状态才能取消
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        // 回滚库存
        productFeignClient.rollbackStock(order.getProductId(), order.getQuantity());

        // 更新订单状态
        order.setStatus(2); // 已取消
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("取消订单成功，订单号：{}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        
        // 验证用户
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 只有待支付状态才能支付
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }

        // 更新订单状态
        order.setStatus(1); // 已支付
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        log.info("支付订单成功，订单号：{}", orderNo);
    }
}



