package com.ecommerce.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<Product> getProductList(Integer pageNum, Integer pageSize, String category) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Product::getCategory, category);
        }
        
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(page, wrapper);
    }

    @Override
    public Product getProductById(Long productId) {
        // 先从缓存查询
        String cacheKey = RedisKeyConstant.PRODUCT_DETAIL_KEY + productId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSON.parseObject(cacheValue, Product.class);
        }

        // 缓存未命中，查询数据库
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 写入缓存
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
        return product;
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        int rows = productMapper.deductStock(productId, quantity);
        if (rows > 0) {
            log.info("扣减库存成功，商品ID：{}，数量：{}", productId, quantity);
            // 删除缓存
            stringRedisTemplate.delete(RedisKeyConstant.PRODUCT_DETAIL_KEY + productId);
            return true;
        }
        log.warn("扣减库存失败，库存不足，商品ID：{}，数量：{}", productId, quantity);
        return false;
    }

    @Override
    public boolean rollbackStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setStock(product.getStock() + quantity);
            product.setSales(product.getSales() - quantity);
            productMapper.updateById(product);
            
            log.info("回滚库存成功，商品ID：{}，数量：{}", productId, quantity);
            // 删除缓存
            stringRedisTemplate.delete(RedisKeyConstant.PRODUCT_DETAIL_KEY + productId);
            return true;
        }
        return false;
    }
}



