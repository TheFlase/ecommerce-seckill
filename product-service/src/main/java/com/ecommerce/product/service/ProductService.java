package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.product.entity.Product;

/**
 * 商品服务
 */
public interface ProductService {

    Page<Product> getProductList(Integer pageNum, Integer pageSize, String category);

    Product getProductById(Long productId);

    /** 幂等扣减：bizNo 唯一标识一次扣减 */
    boolean deductStock(Long productId, Integer quantity, String bizNo);

    /**
     * 幂等回补：bizNo 唯一标识一次回补。
     * deductBizNo 非空时，仅当存在对应 DEDUCT 记录才真正加库存。
     */
    boolean rollbackStock(Long productId, Integer quantity, String bizNo, String deductBizNo);
}
