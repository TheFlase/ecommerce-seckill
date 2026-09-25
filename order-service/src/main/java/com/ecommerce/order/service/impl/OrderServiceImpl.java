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
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductFeignClient productFeignClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Order createOrder(CreateOrderVO createOrderVO) {
        if (createOrderVO.getQuantity() == null || createOrderVO.getQuantity() <= 0) {
            throw new BusinessException("购买数量必须为正整数");
        }
        if (createOrderVO.getProductId() == null) {
            throw new BusinessException("商品ID不能为空");
        }

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

        // 预生成订单号，作为扣减幂等键
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        Long productId = product.getId();
        Integer quantity = createOrderVO.getQuantity();

        try {
            Result<Boolean> deductResult = productFeignClient.deductStock(productId, quantity, orderNo);
            if (deductResult == null || deductResult.getData() == null || !deductResult.getData()) {
                throw new BusinessException("库存不足");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 远程调用异常（含超时）：可能已扣减，幂等回补
            safeRollbackOnCreate(productId, quantity, orderNo);
            throw new BusinessException("扣减库存失败，请稍后重试");
        }

        try {
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(createOrderVO.getUserId());
            order.setProductId(product.getId());
            order.setProductName(product.getProductName());
            order.setQuantity(quantity);
            order.setPrice(product.getCurrentPrice());
            order.setTotalPrice(product.getCurrentPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setStatus(0);
            order.setOrderType(0);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            transactionTemplate.executeWithoutResult(status -> orderMapper.insert(order));
            log.info("创建订单成功，订单号：{}", order.getOrderNo());
            return order;
        } catch (Exception e) {
            safeRollbackOnCreate(productId, quantity, orderNo);
            throw e;
        }
    }

    private void safeRollbackOnCreate(Long productId, Integer quantity, String orderNo) {
        try {
            productFeignClient.rollbackStock(productId, quantity, "create-rb:" + orderNo, orderNo);
        } catch (Exception ex) {
            log.error("下单失败后回补库存失败，需对账。orderNo={}, productId={}, qty={}",
                    orderNo, productId, quantity, ex);
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

        int rows = orderMapper.cancelIfPending(orderNo, userId);
        if (rows <= 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        try {
            Result<Boolean> rollback = productFeignClient.rollbackStock(
                    order.getProductId(),
                    order.getQuantity(),
                    "cancel:" + orderNo,
                    orderNo
            );
            if (rollback == null || rollback.getData() == null || !rollback.getData()) {
                log.error("取消订单后库存回补未生效，需对账。orderNo={}", orderNo);
            }
        } catch (Exception ex) {
            log.error("取消订单后库存回补调用失败，需对账。orderNo={}", orderNo, ex);
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
