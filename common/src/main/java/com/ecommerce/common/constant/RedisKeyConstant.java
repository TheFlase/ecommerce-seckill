package com.ecommerce.common.constant;

/**
 * Redis Key常量
 */
public class RedisKeyConstant {
    
    /** 秒杀商品库存前缀 */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    
    /** 秒杀商品信息前缀 */
    public static final String SECKILL_PRODUCT_KEY = "seckill:product:";
    
    /** 秒杀订单前缀 */
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    
    /** 用户秒杀记录前缀 (防止重复购买) */
    public static final String USER_SECKILL_KEY = "seckill:user:";
    
    /** 分布式锁前缀 */
    public static final String LOCK_KEY = "lock:";
    
    /** 商品详情缓存前缀 */
    public static final String PRODUCT_DETAIL_KEY = "product:detail:";
    
    /** 用户信息缓存前缀 */
    public static final String USER_INFO_KEY = "user:info:";
    
    /** 令牌桶限流前缀 */
    public static final String RATE_LIMIT_KEY = "rate:limit:";
}



