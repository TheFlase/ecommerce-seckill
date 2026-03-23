package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public Result<Page<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String category) {
        Page<Product> page = productService.getProductList(pageNum, pageSize, category);
        return Result.success(page);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/detail/{productId}")
    public Result<Product> getProductDetail(@PathVariable Long productId) {
        Product product = productService.getProductById(productId);
        return Result.success(product);
    }

    /**
     * 扣减库存（内部调用）
     */
    @PostMapping("/deduct-stock")
    public Result<Boolean> deductStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        boolean success = productService.deductStock(productId, quantity);
        return Result.success(success);
    }

    /**
     * 回滚库存（内部调用）
     */
    @PostMapping("/rollback-stock")
    public Result<Boolean> rollbackStock(@RequestParam Long productId, @RequestParam Integer quantity) {
        boolean success = productService.rollbackStock(productId, quantity);
        return Result.success(success);
    }
}



