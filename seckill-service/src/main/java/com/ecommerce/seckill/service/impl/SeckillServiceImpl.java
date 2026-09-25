package com.ecommerce.seckill.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.seckill.entity.SeckillActivity;
import com.ecommerce.seckill.mapper.SeckillActivityMapper;
import com.ecommerce.seckill.service.SeckillService;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现
 *
 * 高并发方案：
 * 1. Redis 库存预热
 * 2. Lua 原子扣减 Redis 库存
 * 3. 固定窗口限流
 * 4. 用户购买标记防重
 * 5. RabbitMQ 异步落库；消费者侧用 Redisson + DB 乐观锁扣库存，失败回补 Redis
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    // Lua脚本：扣减库存（保证原子性）
    private static final String DEDUCT_STOCK_SCRIPT =
            "if redis.call('exists', KEYS[1]) == 1 then " +
            "    local stock = tonumber(redis.call('get', KEYS[1])); " +
            "    if stock >= tonumber(ARGV[1]) then " +
            "        redis.call('decrby', KEYS[1], ARGV[1]); " +
            "        return 1; " +
            "    else " +
            "        return 0; " +
            "    end " +
            "else " +
            "    return -1; " +
            "end";

    @Override
    public List<SeckillActivity> getActiveList() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillActivity::getStatus, 1)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now)
                .orderByDesc(SeckillActivity::getCreateTime);
        return seckillActivityMapper.selectList(wrapper);
    }

    @Override
    public SeckillActivity getActivityById(Long activityId) {
        // 先从缓存获取
        String cacheKey = RedisKeyConstant.SECKILL_PRODUCT_KEY + activityId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cacheValue != null) {
            return JSON.parseObject(cacheValue, SeckillActivity.class);
        }

        // 缓存未命中，查询数据库
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }

        // 写入缓存
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(activity), 30, TimeUnit.MINUTES);
        return activity;
    }

    @Override
    public void warmUpSeckillStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }

        // 将库存加载到Redis
        String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + activityId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getSeckillStock()));
        
        // 缓存活动信息
        String activityKey = RedisKeyConstant.SECKILL_PRODUCT_KEY + activityId;
        stringRedisTemplate.opsForValue().set(activityKey, JSON.toJSONString(activity), 
                30, TimeUnit.MINUTES);

        log.info("秒杀库存预热成功，活动ID：{}，库存：{}", activityId, activity.getSeckillStock());
    }

    @Override
    @SentinelResource(value = "doSeckill", blockHandler = "doSeckillBlockHandler")
    public String doSeckill(Long activityId, Long userId, Integer quantity) {
        // 1. 校验活动状态
        SeckillActivity activity = getActivityById(activityId);
        if (activity.getStatus() != 1) {
            throw new BusinessException("秒杀活动未开始或已结束");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException("秒杀活动未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("秒杀活动已结束");
        }

        // 2. 校验购买数量
        if (quantity > activity.getLimitPerUser()) {
            throw new BusinessException("超过限购数量");
        }

        // 3. 检查是否已经购买过（防重）
        String userSeckillKey = RedisKeyConstant.USER_SECKILL_KEY + activityId + ":" + userId;
        Boolean isFirstTime = stringRedisTemplate.opsForValue().setIfAbsent(
                userSeckillKey, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isFirstTime)) {
            throw new BusinessException("您已经参与过该秒杀活动");
        }

        // 4. Lua 原子扣减 Redis 库存（限流交给 Sentinel）
        String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + activityId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_STOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(stockKey),
                String.valueOf(quantity));

        if (result == null || result == -1) {
            stringRedisTemplate.delete(userSeckillKey);
            throw new BusinessException("系统异常，请稍后重试");
        } else if (result == 0) {
            stringRedisTemplate.delete(userSeckillKey);
            throw new BusinessException("商品已抢光");
        }

        String orderNo = IdUtil.getSnowflakeNextIdStr();

        SeckillOrderVO orderVO = new SeckillOrderVO();
        orderVO.setOrderNo(orderNo);
        orderVO.setUserId(userId);
        orderVO.setActivityId(activityId);
        orderVO.setProductId(activity.getProductId());
        orderVO.setProductName(activity.getProductName());
        orderVO.setQuantity(quantity);
        orderVO.setSeckillPrice(activity.getSeckillPrice());

        try {
            rabbitTemplate.convertAndSend(
                    MqConstant.SECKILL_ORDER_EXCHANGE,
                    MqConstant.SECKILL_ORDER_ROUTING_KEY,
                    JSON.toJSONString(orderVO)
            );
        } catch (Exception e) {
            log.error("秒杀消息发送失败，回补库存。orderNo={}", orderNo, e);
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
            stringRedisTemplate.delete(userSeckillKey);
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        log.info("秒杀成功，订单号：{}，用户ID：{}，活动ID：{}", orderNo, userId, activityId);
        return orderNo;
    }

    public String doSeckillBlockHandler(Long activityId, Long userId, Integer quantity, BlockException ex) {
        throw new BusinessException("系统繁忙，请稍后重试（Sentinel限流）");
    }
}



