package com.ecommerce.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀活动Mapper
 */
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {
    
    /**
     * 扣减秒杀库存（乐观锁）
     */
    @Update("UPDATE t_seckill_activity SET seckill_stock = seckill_stock - #{quantity}, " +
            "sold_count = sold_count + #{quantity} " +
            "WHERE id = #{activityId} AND seckill_stock >= #{quantity}")
    int deductSeckillStock(Long activityId, Integer quantity);
}



