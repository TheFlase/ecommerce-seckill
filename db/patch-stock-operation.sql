-- 已有库增量：库存操作幂等表
USE ecommerce_seckill;

CREATE TABLE IF NOT EXISTS t_stock_operation (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    op_type VARCHAR(16) NOT NULL COMMENT 'DEDUCT/ROLLBACK',
    product_id BIGINT(20) NOT NULL COMMENT '商品ID',
    quantity INT(11) NOT NULL COMMENT '数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_op (biz_no, op_type),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存操作幂等表';
