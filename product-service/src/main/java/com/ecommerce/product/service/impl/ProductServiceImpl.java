package com.ecommerce.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.StockOperation;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.mapper.StockOperationMapper;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private static final String OP_DEDUCT = "DEDUCT";
    private static final String OP_ROLLBACK = "ROLLBACK";

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StockOperationMapper stockOperationMapper;

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
        String cacheKey = RedisKeyConstant.PRODUCT_DETAIL_KEY + productId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSON.parseObject(cacheValue, Product.class);
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Integer quantity, String bizNo) {
        validateQuantity(quantity);
        if (StrUtil.isBlank(bizNo)) {
            throw new BusinessException("bizNo 不能为空");
        }
        if (stockOperationMapper.countByBizNoAndType(bizNo, OP_DEDUCT) > 0) {
            log.info("扣减幂等跳过，bizNo={}", bizNo);
            return true;
        }
        try {
            insertOperation(bizNo, OP_DEDUCT, productId, quantity);
        } catch (DuplicateKeyException e) {
            return true;
        }
        int rows = productMapper.deductStock(productId, quantity);
        if (rows <= 0) {
            throw new BusinessException("库存不足");
        }
        evictCache(productId);
        log.info("扣减库存成功，productId={}, qty={}, bizNo={}", productId, quantity, bizNo);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackStock(Long productId, Integer quantity, String bizNo, String deductBizNo) {
        validateQuantity(quantity);
        if (StrUtil.isBlank(bizNo)) {
            throw new BusinessException("bizNo 不能为空");
        }
        if (StrUtil.isNotBlank(deductBizNo)
                && stockOperationMapper.countByBizNoAndType(deductBizNo, OP_DEDUCT) == 0) {
            log.warn("无对应扣减记录，跳过回补。deductBiz={}, rollbackBiz={}", deductBizNo, bizNo);
            return false;
        }
        if (stockOperationMapper.countByBizNoAndType(bizNo, OP_ROLLBACK) > 0) {
            log.info("回补幂等跳过，bizNo={}", bizNo);
            return true;
        }
        try {
            insertOperation(bizNo, OP_ROLLBACK, productId, quantity);
        } catch (DuplicateKeyException e) {
            return true;
        }
        int rows = productMapper.rollbackStock(productId, quantity);
        if (rows <= 0) {
            throw new BusinessException("商品不存在");
        }
        evictCache(productId);
        log.info("回滚库存成功，productId={}, qty={}, bizNo={}", productId, quantity, bizNo);
        return true;
    }

    private void insertOperation(String bizNo, String opType, Long productId, Integer quantity) {
        StockOperation op = new StockOperation();
        op.setBizNo(bizNo);
        op.setOpType(opType);
        op.setProductId(productId);
        op.setQuantity(quantity);
        op.setCreateTime(LocalDateTime.now());
        stockOperationMapper.insert(op);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("数量必须为正整数");
        }
    }

    private void evictCache(Long productId) {
        stringRedisTemplate.delete(RedisKeyConstant.PRODUCT_DETAIL_KEY + productId);
    }
}
