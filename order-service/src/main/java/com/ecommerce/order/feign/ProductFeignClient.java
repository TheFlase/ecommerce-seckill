package com.ecommerce.order.feign;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/product/detail/{productId}")
    Result<ProductDTO> getProductDetail(@PathVariable("productId") Long productId);

    @PostMapping("/product/deduct-stock")
    Result<Boolean> deductStock(@RequestParam("productId") Long productId,
                                @RequestParam("quantity") Integer quantity,
                                @RequestParam("bizNo") String bizNo);

    @PostMapping("/product/rollback-stock")
    Result<Boolean> rollbackStock(@RequestParam("productId") Long productId,
                                  @RequestParam("quantity") Integer quantity,
                                  @RequestParam("bizNo") String bizNo,
                                  @RequestParam(value = "deductBizNo", required = false) String deductBizNo);
}
