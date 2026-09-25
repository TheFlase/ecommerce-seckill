package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.mq.SeckillRedisCompensator.CompensateResult;
import com.ecommerce.seckill.service.SeckillOrderPersistService;
import com.ecommerce.seckill.service.SeckillOrderPersistService.PersistResult;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单消费者：落库与通信失败分离；仅落库未提交时才回补 Redis。
 */
@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private SeckillOrderPersistService persistService;

    @Resource
    private SeckillRedisCompensator redisCompensator;

    @Resource
    private RedissonClient redissonClient;

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody());
        SeckillOrderVO orderVO = null;
        boolean persistCommitted = false;

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
                        persistCommitted = true;
                        channel.basicAck(deliveryTag, false);
                        return;
                    case STOCK_SHORT:
                        log.warn("DB秒杀库存不足，回补Redis。orderNo={}", orderVO.getOrderNo());
                        ackAfterCompensate(channel, deliveryTag, orderVO);
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
            if (persistCommitted) {
                // 落库已提交：禁止回补 Redis，仅重投消息等待 ACK
                log.error("落库已提交但后续处理失败，跳过 Redis 回补。orderNo={}",
                        orderVO != null ? orderVO.getOrderNo() : "unknown", e);
                channel.basicNack(deliveryTag, false, true);
                return;
            }
            if (orderVO != null) {
                if (redisCompensator.isCompensateDone(orderVO.getOrderNo())) {
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    ackAfterCompensate(channel, deliveryTag, orderVO);
                }
            } else {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private void ackAfterCompensate(Channel channel, long deliveryTag, SeckillOrderVO orderVO)
            throws Exception {
        CompensateResult cr = redisCompensator.compensate(orderVO);
        if (cr == CompensateResult.FAILED) {
            channel.basicNack(deliveryTag, false, false);
        } else {
            channel.basicAck(deliveryTag, false);
        }
    }
}
