package com.ecommerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单Mapper
 */
public interface OrderMapper extends BaseMapper<Order> {

    /** 待支付 → 已取消（条件更新，防并发重复回库存） */
    @Update("UPDATE t_order SET status = 2, update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int cancelIfPending(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    /** 待支付 → 已支付 */
    @Update("UPDATE t_order SET status = 1, update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND user_id = #{userId} AND status = 0")
    int payIfPending(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
