package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.service.SeckillOrderPersistService;
import com.ecommerce.seckill.service.SeckillOrderPersistService.PersistResult;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单消费者：本地事务落库；失败回补 Redis；补偿失败进 DLQ 保留消息。
 */
@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private SeckillOrderPersistService persistService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody());
        SeckillOrderVO orderVO = null;

        try {
            log.info("接收到秒杀订单消息：{}", messageBody);
            orderVO = JSON.parseObject(messageBody, SeckillOrderVO.class);

            String lockKey = RedisKeyConstant.LOCK_KEY + "seckill:order:" + orderVO.getOrderNo();
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!locked) {
                    channel.basicNack(deliveryTag, false, true);
                    return;
                }

                PersistResult result = persistService.persist(orderVO);
                switch (result) {
                    case SUCCESS:
                    case ALREADY_EXISTS:
                        channel.basicAck(deliveryTag, false);
                        return;
                    case STOCK_SHORT:
                        log.warn("DB秒杀库存不足，回补Redis。orderNo={}", orderVO.getOrderNo());
                        if (!compensateRedis(orderVO)) {
                            // 补偿失败：进 DLQ，保留消息待人工/对账
                            channel.basicNack(deliveryTag, false, false);
                            return;
                        }
                        channel.basicAck(deliveryTag, false);
                        return;
                    default:
                        channel.basicNack(deliveryTag, false, false);
                }
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("处理秒杀订单消息失败：{}", e.getMessage(), e);
            // 本地事务已回滚；仅需回补 Redis。补偿失败则 Nack→DLQ，禁止 ACK 丢消息。
            if (orderVO != null && compensateRedis(orderVO)) {
                channel.basicAck(deliveryTag, false);
            } else {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    /**
     * @return true 补偿成功；false 失败（已写入 Redis 失败标记）
     */
    private boolean compensateRedis(SeckillOrderVO orderVO) {
        try {
            String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + orderVO.getActivityId();
            stringRedisTemplate.opsForValue().increment(stockKey, orderVO.getQuantity());

            String userSeckillKey = RedisKeyConstant.USER_SECKILL_KEY
                    + orderVO.getActivityId() + ":" + orderVO.getUserId();
            stringRedisTemplate.delete(userSeckillKey);

            stringRedisTemplate.delete(compensateFailKey(orderVO.getOrderNo()));
            log.info("已回补Redis库存并清除用户秒杀标记，activityId={}, userId={}, qty={}",
                    orderVO.getActivityId(), orderVO.getUserId(), orderVO.getQuantity());
            return true;
        } catch (Exception ex) {
            log.error("回补Redis失败，写入补偿标记。orderNo={}", orderVO.getOrderNo(), ex);
            try {
                stringRedisTemplate.opsForValue().set(
                        compensateFailKey(orderVO.getOrderNo()),
                        JSON.toJSONString(orderVO),
                        7, TimeUnit.DAYS
                );
            } catch (Exception ignore) {
                log.error("写入补偿失败标记也失败，orderNo={}", orderVO.getOrderNo());
            }
            return false;
        }
    }

    private static String compensateFailKey(String orderNo) {
        return "seckill:compensate:fail:" + orderNo;
    }
}
