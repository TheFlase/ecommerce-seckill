package com.ecommerce.seckill.config;

import com.ecommerce.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ：业务队列 + 死信队列
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(MqConstant.SECKILL_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange seckillOrderDlx() {
        return new DirectExchange(MqConstant.SECKILL_ORDER_DLX, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstant.SECKILL_ORDER_DLX);
        args.put("x-dead-letter-routing-key", MqConstant.SECKILL_ORDER_DLQ_ROUTING_KEY);
        return QueueBuilder.durable(MqConstant.SECKILL_ORDER_QUEUE).withArguments(args).build();
    }

    @Bean
    public Queue seckillOrderDlq() {
        return QueueBuilder.durable(MqConstant.SECKILL_ORDER_DLQ).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(MqConstant.SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding seckillOrderDlqBinding() {
        return BindingBuilder.bind(seckillOrderDlq())
                .to(seckillOrderDlx())
                .with(MqConstant.SECKILL_ORDER_DLQ_ROUTING_KEY);
    }
}
