package com.ecommerce.common.constant;

/**
 * MQ常量
 */
public class MqConstant {

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    /** 死信交换机 / 队列 */
    public static final String SECKILL_ORDER_DLX = "seckill.order.dlx";
    public static final String SECKILL_ORDER_DLQ = "seckill.order.dlq";
    public static final String SECKILL_ORDER_DLQ_ROUTING_KEY = "seckill.order.dead";

    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final Integer ORDER_TIMEOUT = 900000;
}
