package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.entity.SeckillOrder;
import com.ecommerce.seckill.mapper.SeckillActivityMapper;
import com.ecommerce.seckill.mapper.SeckillOrderMapper;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单消费者：异步落库 + 扣减 DB 库存，失败则回补 Redis。
 */
@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

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
                    // 短暂拿不到锁：重新入队，避免并发重复消费
                    channel.basicNack(deliveryTag, false, true);
                    return;
                }

                // 幂等：订单号已存在则直接 ACK
                SeckillOrder existing = seckillOrderMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                                .eq(SeckillOrder::getOrderNo, orderVO.getOrderNo())
                );
                if (existing != null) {
                    channel.basicAck(deliveryTag, false);
                    return;
                }

                // 扣减数据库秒杀库存（乐观锁）
                int updated = seckillActivityMapper.deductSeckillStock(
                        orderVO.getActivityId(), orderVO.getQuantity());
                if (updated <= 0) {
                    log.warn("DB秒杀库存不足，回补Redis。orderNo={}", orderVO.getOrderNo());
                    compensateRedis(orderVO);
                    channel.basicAck(deliveryTag, false);
                    return;
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
                        new BigDecimal(orderVO.getQuantity())
                ));
                order.setStatus(0);
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                seckillOrderMapper.insert(order);

                log.info("秒杀订单创建成功：{}", order.getOrderNo());
                channel.basicAck(deliveryTag, false);
            } finally {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("处理秒杀订单消息失败：{}", e.getMessage(), e);
            // 避免无限 requeue 打满队列：失败回补后 ACK（学习项目策略）
            if (orderVO != null) {
                compensateRedis(orderVO);
            }
            channel.basicAck(deliveryTag, false);
        }
    }

    private void compensateRedis(SeckillOrderVO orderVO) {
        try {
            String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + orderVO.getActivityId();
            stringRedisTemplate.opsForValue().increment(stockKey, orderVO.getQuantity());

            String userSeckillKey = RedisKeyConstant.USER_SECKILL_KEY
                    + orderVO.getActivityId() + ":" + orderVO.getUserId();
            stringRedisTemplate.delete(userSeckillKey);
            log.info("已回补Redis库存并清除用户秒杀标记，activityId={}, userId={}, qty={}",
                    orderVO.getActivityId(), orderVO.getUserId(), orderVO.getQuantity());
        } catch (Exception ex) {
            log.error("回补Redis失败，需要人工介入。orderNo={}", orderVO.getOrderNo(), ex);
        }
    }
}
