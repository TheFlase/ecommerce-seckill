package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @GetMapping("/list")
    public Result<Page<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category) {
        return Result.success(productService.getProductList(pageNum, pageSize, category));
    }

    @GetMapping("/detail/{productId}")
    public Result<Product> getProductDetail(@PathVariable Long productId) {
        return Result.success(productService.getProductById(productId));
    }

    @PostMapping("/deduct-stock")
    public Result<Boolean> deductStock(@RequestParam Long productId,
                                       @RequestParam Integer quantity,
                                       @RequestParam String bizNo) {
        return Result.success(productService.deductStock(productId, quantity, bizNo));
    }

    @PostMapping("/rollback-stock")
    public Result<Boolean> rollbackStock(@RequestParam Long productId,
                                         @RequestParam Integer quantity,
                                         @RequestParam String bizNo,
                                         @RequestParam(required = false) String deductBizNo) {
        return Result.success(productService.rollbackStock(productId, quantity, bizNo, deductBizNo));
    }
}
