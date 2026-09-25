package com.ecommerce.seckill.service;

import com.ecommerce.seckill.entity.SeckillActivity;

import java.util.List;

/**
 * 秒杀服务
 */
public interface SeckillService {

    List<SeckillActivity> getActiveList();

    SeckillActivity getActivityById(Long activityId);

    void warmUpSeckillStock(Long activityId);

    /**
     * 执行秒杀（限流由 Sentinel 负责）
     */
    String doSeckill(Long activityId, Long userId, Integer quantity);
}
