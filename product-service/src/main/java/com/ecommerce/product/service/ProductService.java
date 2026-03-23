package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.entity.Product;

/**
 * 商品服务
 */
public interface ProductService {
    
    /**
     * 分页查询商品列表
     */
    Page<Product> getProductList(Integer pageNum, Integer pageSize, String category);

    /**
     * 根据ID查询商品详情
     */
    Product getProductById(Long productId);

    /**
     * 扣减库存
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 回滚库存
     */
    boolean rollbackStock(Long productId, Integer quantity);
}



