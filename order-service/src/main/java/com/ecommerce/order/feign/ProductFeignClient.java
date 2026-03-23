package com.ecommerce.order.feign;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "product-service")
public interface ProductFeignClient {

    /**
     * 扣减库存
     */
    @PostMapping("/product/deduct-stock")
    Result<Boolean> deductStock(@RequestParam("productId") Long productId, 
                               @RequestParam("quantity") Integer quantity);

    /**
     * 回滚库存
     */
    @PostMapping("/product/rollback-stock")
    Result<Boolean> rollbackStock(@RequestParam("productId") Long productId, 
                                 @RequestParam("quantity") Integer quantity);
}



