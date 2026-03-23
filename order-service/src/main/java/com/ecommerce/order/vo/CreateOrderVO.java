package com.ecommerce.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建订单VO
 */
@Data
public class CreateOrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
}



