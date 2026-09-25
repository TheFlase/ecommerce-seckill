package com.ecommerce.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.ProductDTO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.feign.ProductFeignClient;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.vo.CreateOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
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
        if (createOrderVO.getQuantity() == null || createOrderVO.getQuantity() <= 0) {
            throw new BusinessException("购买数量必须为正整数");
        }
        if (createOrderVO.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }

        // 价格/名称以服务端商品为准，不信任客户端
        Result<ProductDTO> productResult = productFeignClient.getProductDetail(createOrderVO.getProductId());
        ProductDTO product = productResult == null ? null : productResult.getData();
        if (product == null || product.getId() == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStatus() != null && product.getStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
        if (product.getCurrentPrice() == null || product.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品价格异常");
        }

        Result<Boolean> deductResult = productFeignClient.deductStock(
                createOrderVO.getProductId(),
                createOrderVO.getQuantity()
        );
        if (deductResult.getData() == null || !deductResult.getData()) {
            throw new BusinessException("库存不足");
        }

        try {
            Order order = new Order();
            order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
            order.setUserId(createOrderVO.getUserId());
            order.setProductId(product.getId());
            order.setProductName(product.getProductName());
            order.setQuantity(createOrderVO.getQuantity());
            order.setPrice(product.getCurrentPrice());
            order.setTotalPrice(product.getCurrentPrice().multiply(
                    BigDecimal.valueOf(createOrderVO.getQuantity())
            ));
            order.setStatus(0);
            order.setOrderType(0);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            orderMapper.insert(order);
            log.info("创建订单成功，订单号：{}", order.getOrderNo());
            return order;
        } catch (RuntimeException e) {
            // 本地事务无法回滚远程扣减，尽力补偿
            try {
                productFeignClient.rollbackStock(createOrderVO.getProductId(), createOrderVO.getQuantity());
            } catch (Exception ex) {
                log.error("下单失败后回补库存也失败，productId={}, qty={}",
                        createOrderVO.getProductId(), createOrderVO.getQuantity(), ex);
            }
            throw e;
        }
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
    public Order getOrderByOrderNo(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 先条件更新状态，再回库存，避免并发双回补
        int rows = orderMapper.cancelIfPending(orderNo, userId);
        if (rows <= 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        Result<Boolean> rollback = productFeignClient.rollbackStock(order.getProductId(), order.getQuantity());
        if (rollback == null || rollback.getData() == null || !rollback.getData()) {
            log.error("取消订单后库存回补失败，需人工处理。orderNo={}, productId={}, qty={}",
                    orderNo, order.getProductId(), order.getQuantity());
        }

        log.info("取消订单成功，订单号：{}", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        int rows = orderMapper.payIfPending(orderNo, userId);
        if (rows <= 0) {
            throw new BusinessException("订单状态不允许支付");
        }

        log.info("支付订单成功，订单号：{}", orderNo);
    }
}
