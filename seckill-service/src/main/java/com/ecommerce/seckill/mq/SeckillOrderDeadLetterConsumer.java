package com.ecommerce.seckill.mq;

import com.ecommerce.common.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀死信消费者：记录无法处理的消息，便于人工/对账介入
 */
@Slf4j
@Component
public class SeckillOrderDeadLetterConsumer {

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_DLQ)
    public void handleDeadLetter(Message message) {
        String body = new String(message.getBody());
        log.error("[DLQ] seckill order dead letter received: headers={}, body={}",
                message.getMessageProperties().getHeaders(), body);
    }
}
