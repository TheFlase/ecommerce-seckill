package com.ecommerce.seckill.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.entity.SeckillActivity;
import com.ecommerce.seckill.mapper.SeckillActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * Redis 与 DB 秒杀库存对账（默认只告警，不自动纠偏）
 */
@Slf4j
@Component
public class SeckillStockReconcileJob {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${seckill.reconcile.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${seckill.reconcile.cron:0 */5 * * * ?}")
    public void reconcile() {
        if (!enabled) {
            return;
        }
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
                new LambdaQueryWrapper<SeckillActivity>().eq(SeckillActivity::getStatus, 1)
        );
        for (SeckillActivity activity : activities) {
            String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + activity.getId();
            String redisVal = stringRedisTemplate.opsForValue().get(stockKey);
            if (redisVal == null) {
                continue;
            }
            int redisStock = Integer.parseInt(redisVal);
            int dbStock = activity.getSeckillStock() == null ? 0 : activity.getSeckillStock();
            if (redisStock != dbStock) {
                log.warn("[RECONCILE] activityId={} redisStock={} dbStock={} soldCount={}",
                        activity.getId(), redisStock, dbStock, activity.getSoldCount());
            }
        }
    }
}
