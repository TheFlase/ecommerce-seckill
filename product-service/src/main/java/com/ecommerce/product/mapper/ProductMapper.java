package com.ecommerce.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.product.entity.Product;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper
 */
public interface ProductMapper extends BaseMapper<Product> {
    
    /**
     * 扣减库存（乐观锁）
     */
    @Update("UPDATE t_product SET stock = stock - #{quantity}, sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(Long productId, Integer quantity);
}



