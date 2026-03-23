package com.ecommerce.seckill.config;

import com.ecommerce.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 秒杀订单交换机
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(MqConstant.SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * 秒杀订单队列
     */
    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(MqConstant.SECKILL_ORDER_QUEUE, true);
    }

    /**
     * 绑定秒杀订单队列到交换机
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(MqConstant.SECKILL_ORDER_ROUTING_KEY);
    }
}



