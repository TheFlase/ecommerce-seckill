package com.ecommerce.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品服务 Feign 反序列化 DTO（仅取下单所需字段）
 */
@Data
public class ProductDTO {
    private Long id;
    private String productName;
    private BigDecimal currentPrice;
    private Integer status;
}
