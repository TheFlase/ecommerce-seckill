package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀死信消费者：记录失败消息，并尽力回补 Redis（幂等：有补偿失败标记才回补）。
 */
@Slf4j
@Component
public class SeckillOrderDeadLetterConsumer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_DLQ)
    public void handleDeadLetter(Message message) {
        String body = new String(message.getBody());
        log.error("[DLQ] seckill order dead letter received: headers={}, body={}",
                message.getMessageProperties().getHeaders(), body);
        try {
            SeckillOrderVO orderVO = JSON.parseObject(body, SeckillOrderVO.class);
            if (orderVO == null || orderVO.getOrderNo() == null) {
                return;
            }
            String failKey = "seckill:compensate:fail:" + orderVO.getOrderNo();
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(failKey))) {
                // 无失败标记：可能已补偿成功，只留审计日志
                return;
            }
            String stockKey = RedisKeyConstant.SECKILL_STOCK_KEY + orderVO.getActivityId();
            stringRedisTemplate.opsForValue().increment(stockKey, orderVO.getQuantity());
            stringRedisTemplate.delete(RedisKeyConstant.USER_SECKILL_KEY
                    + orderVO.getActivityId() + ":" + orderVO.getUserId());
            stringRedisTemplate.delete(failKey);
            log.warn("[DLQ] compensated Redis for orderNo={}", orderVO.getOrderNo());
        } catch (Exception e) {
            log.error("[DLQ] compensate retry failed, keep fail mark. body={}", body, e);
            try {
                stringRedisTemplate.opsForValue().set(
                        "seckill:compensate:fail:dlq:" + System.currentTimeMillis(),
                        body, 7, TimeUnit.DAYS);
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}
