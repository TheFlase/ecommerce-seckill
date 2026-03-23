package com.ecommerce.seckill.service;

import com.ecommerce.seckill.entity.SeckillActivity;
import com.ecommerce.seckill.vo.SeckillOrderVO;

import java.util.List;

/**
 * 秒杀服务
 */
public interface SeckillService {
    
    /**
     * 查询进行中的秒杀活动列表
     */
    List<SeckillActivity> getActiveList();

    /**
     * 根据ID查询秒杀活动详情
     */
    SeckillActivity getActivityById(Long activityId);

    /**
     * 秒杀预热 - 将库存加载到Redis
     */
    void warmUpSeckillStock(Long activityId);

    /**
     * 执行秒杀（核心方法）
     */
    String doSeckill(Long activityId, Long userId, Integer quantity);

    /**
     * 获取秒杀令牌（令牌桶限流）
     */
    boolean acquireSeckillToken(Long activityId);
}



