package com.ecommerce.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.seckill.entity.SeckillOrder;
import com.ecommerce.seckill.mapper.SeckillActivityMapper;
import com.ecommerce.seckill.mapper.SeckillOrderMapper;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀落库：DB 扣库存 + 插单同一本地事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderPersistService {

    public enum PersistResult {
        SUCCESS,
        ALREADY_EXISTS,
        STOCK_SHORT
    }

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillActivityMapper seckillActivityMapper;

    @Transactional(rollbackFor = Exception.class)
    public PersistResult persist(SeckillOrderVO orderVO) {
        SeckillOrder existing = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getOrderNo, orderVO.getOrderNo())
        );
        if (existing != null) {
            return PersistResult.ALREADY_EXISTS;
        }

        int updated = seckillActivityMapper.deductSeckillStock(
                orderVO.getActivityId(), orderVO.getQuantity());
        if (updated <= 0) {
            return PersistResult.STOCK_SHORT;
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderVO.getOrderNo());
        order.setUserId(orderVO.getUserId());
        order.setActivityId(orderVO.getActivityId());
        order.setProductId(orderVO.getProductId());
        order.setProductName(orderVO.getProductName());
        order.setQuantity(orderVO.getQuantity());
        order.setSeckillPrice(orderVO.getSeckillPrice());
        order.setTotalPrice(orderVO.getSeckillPrice().multiply(
                BigDecimal.valueOf(orderVO.getQuantity())
        ));
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        seckillOrderMapper.insert(order);

        log.info("秒杀订单落库成功：{}", order.getOrderNo());
        return PersistResult.SUCCESS;
    }
}
