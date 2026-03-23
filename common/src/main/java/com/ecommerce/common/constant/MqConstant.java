package com.ecommerce.common.constant;

/**
 * MQ常量
 */
public class MqConstant {
    
    /** 秒杀订单交换机 */
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    
    /** 秒杀订单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    
    /** 秒杀订单路由键 */
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";
    
    /** 订单超时取消延迟交换机 */
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    
    /** 订单超时取消队列 */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    
    /** 订单超时取消路由键 */
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    
    /** 订单超时时间（毫秒）15分钟 */
    public static final Integer ORDER_TIMEOUT = 900000;
}



