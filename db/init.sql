-- 创建数据库
CREATE DATABASE IF NOT EXISTS ecommerce_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ecommerce_seckill;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    status INT(1) DEFAULT 0 COMMENT '状态 0-正常 1-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    image_url VARCHAR(500) COMMENT '商品图片',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    current_price DECIMAL(10,2) NOT NULL COMMENT '现价',
    stock INT(11) NOT NULL DEFAULT 0 COMMENT '库存',
    sales INT(11) DEFAULT 0 COMMENT '销量',
    category VARCHAR(50) COMMENT '分类',
    status INT(1) DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    user_id BIGINT(20) NOT NULL COMMENT '用户ID',
    product_id BIGINT(20) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    quantity INT(11) NOT NULL COMMENT '购买数量',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '总价',
    status INT(1) DEFAULT 0 COMMENT '订单状态 0-待支付 1-已支付 2-已取消 3-已完成',
    order_type INT(1) DEFAULT 0 COMMENT '订单类型 0-普通订单 1-秒杀订单',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 秒杀活动表
CREATE TABLE IF NOT EXISTS t_seckill_activity (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    activity_name VARCHAR(200) NOT NULL COMMENT '活动名称',
    product_id BIGINT(20) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    seckill_stock INT(11) NOT NULL COMMENT '秒杀库存',
    sold_count INT(11) DEFAULT 0 COMMENT '已售数量',
    limit_per_user INT(11) DEFAULT 1 COMMENT '每人限购数量',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    status INT(1) DEFAULT 0 COMMENT '状态 0-未开始 1-进行中 2-已结束',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_id (product_id),
    KEY idx_status (status),
    KEY idx_start_time (start_time),
    KEY idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS t_seckill_order (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    user_id BIGINT(20) NOT NULL COMMENT '用户ID',
    activity_id BIGINT(20) NOT NULL COMMENT '活动ID',
    product_id BIGINT(20) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    quantity INT(11) NOT NULL COMMENT '购买数量',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    total_price DECIMAL(10,2) NOT NULL COMMENT '总价',
    status INT(1) DEFAULT 0 COMMENT '订单状态 0-待支付 1-已支付 2-已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_activity_id (activity_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 插入测试数据

-- 插入测试用户
INSERT INTO t_user (username, password, phone, email, nickname, status) VALUES
('testuser1', '5f4dcc3b5aa765d61d8327deb882cf99', '13800138001', 'test1@example.com', '测试用户1', 0),
('testuser2', '5f4dcc3b5aa765d61d8327deb882cf99', '13800138002', 'test2@example.com', '测试用户2', 0),
('admin', '5f4dcc3b5aa765d61d8327deb882cf99', '13800138000', 'admin@example.com', '管理员', 0);

-- 插入测试商品
INSERT INTO t_product (product_name, description, image_url, original_price, current_price, stock, sales, category, status) VALUES
('iPhone 15 Pro Max 256GB', '最新款苹果手机，A17 Pro芯片，钛金属设计', 'https://example.com/iphone15.jpg', 9999.00, 8999.00, 1000, 50, '手机数码', 1),
('MacBook Pro 14 M3', '搭载M3芯片的专业笔记本电脑', 'https://example.com/macbook.jpg', 15999.00, 14999.00, 500, 30, '电脑办公', 1),
('AirPods Pro 3', '主动降噪无线耳机', 'https://example.com/airpods.jpg', 1999.00, 1799.00, 2000, 100, '影音娱乐', 1),
('iPad Air 256GB', '轻薄强大的平板电脑', 'https://example.com/ipad.jpg', 5499.00, 4999.00, 800, 60, '平板电脑', 1),
('Apple Watch Ultra 2', '专业户外运动智能手表', 'https://example.com/watch.jpg', 6999.00, 6499.00, 300, 20, '智能穿戴', 1);

-- 插入秒杀活动
INSERT INTO t_seckill_activity (activity_name, product_id, product_name, original_price, seckill_price, 
                                seckill_stock, sold_count, limit_per_user, start_time, end_time, status) VALUES
('双12手机秒杀', 1, 'iPhone 15 Pro Max 256GB', 9999.00, 6999.00, 100, 0, 1, 
 DATE_ADD(NOW(), INTERVAL -1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1),
('双12笔记本秒杀', 2, 'MacBook Pro 14 M3', 15999.00, 12999.00, 50, 0, 1, 
 DATE_ADD(NOW(), INTERVAL -1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1),
('双12耳机秒杀', 3, 'AirPods Pro 3', 1999.00, 999.00, 500, 0, 2, 
 DATE_ADD(NOW(), INTERVAL -1 HOUR), DATE_ADD(NOW(), INTERVAL 23 HOUR), 1),
('双12平板秒杀', 4, 'iPad Air 256GB', 5499.00, 3999.00, 200, 0, 1, 
 DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 25 HOUR), 0),
('双12手表秒杀', 5, 'Apple Watch Ultra 2', 6999.00, 4999.00, 100, 0, 1, 
 DATE_ADD(NOW(), INTERVAL -25 HOUR), DATE_ADD(NOW(), INTERVAL -1 HOUR), 2);

-- 查询数据
SELECT '用户表数据：' as '表名';
SELECT * FROM t_user;

SELECT '商品表数据：' as '表名';
SELECT * FROM t_product;

SELECT '秒杀活动表数据：' as '表名';
SELECT * FROM t_seckill_activity;

-- 提示信息
SELECT '========================================' as '';
SELECT '数据库初始化完成！' as '提示';
SELECT '默认密码（MD5）: password' as '说明';
SELECT '测试用户: testuser1 / testuser2' as '账号';
SELECT '管理员: admin' as '账号';
SELECT '========================================' as '';



