package com.ecommerce.seckill.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀订单VO
 */
@Data
public class SeckillOrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal seckillPrice;
}



