package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.seckill.mq.SeckillRedisCompensator.CompensateResult;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 秒杀死信消费者：审计 + 幂等 Redis 回补 + 手动 ACK。
 */
@Slf4j
@Component
public class SeckillOrderDeadLetterConsumer {

    @Resource
    private SeckillRedisCompensator redisCompensator;

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_DLQ)
    public void handleDeadLetter(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        log.error("[DLQ] seckill order dead letter: headers={}, body={}",
                message.getMessageProperties().getHeaders(), body);

        try {
            SeckillOrderVO orderVO = JSON.parseObject(body, SeckillOrderVO.class);
            if (orderVO == null || orderVO.getOrderNo() == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 仅对存在失败标记的消息重试回补（Lua 仍保证幂等）
            if (redisCompensator.hasCompensateFailMark(orderVO.getOrderNo())) {
                CompensateResult cr = redisCompensator.compensate(orderVO);
                if (cr == CompensateResult.FAILED) {
                    channel.basicNack(deliveryTag, false, true);
                    return;
                }
                log.warn("[DLQ] compensated Redis for orderNo={}", orderVO.getOrderNo());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[DLQ] handle failed, nack requeue. body={}", body, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
