# 电商秒杀微服务项目

## 项目简介

这是一个基于SpringCloud的电商秒杀微服务系统，主要解决高并发场景下的秒杀业务痛点。项目采用分布式微服务架构，整合了Redis缓存、RabbitMQ消息队列等技术，实现了完整的秒杀业务流程。

## 技术栈

### 核心框架
- **SpringBoot 2.6.13** - 微服务基础框架
- **SpringCloud 2021.0.5** - 微服务治理
- **SpringCloud Alibaba 2021.0.4.0** - 阿里微服务组件

### 数据存储
- **MySQL 8.0** - 关系型数据库
- **Redis 7.0** - 缓存和分布式锁
- **MyBatis-Plus 3.5.3.1** - ORM框架
- **Druid 1.2.16** - 数据库连接池

### 消息队列
- **RabbitMQ 3.11** - 异步消息处理

### 服务治理
- **Eureka** - 服务注册与发现
- **Gateway** - API网关
- **OpenFeign** - 服务间调用

### 其他组件
- **Redisson 3.20.0** - 分布式锁
- **JWT** - 身份认证
- **Lombok** - 简化代码
- **Hutool** - 工具类库

## 项目架构

```
ecommerce-seckill
├── common                  # 公共模块
├── eureka-server          # 注册中心 (8761)
├── gateway                # API网关 (8080)
├── user-service           # 用户服务 (8081)
├── product-service        # 商品服务 (8082)
├── order-service          # 订单服务 (8083)
├── seckill-service        # 秒杀服务 (8084)
├── docker-compose.yml     # Docker编排文件
└── db/init.sql           # 数据库初始化脚本
```

## 核心业务

### 1. 用户服务 (user-service)
- 用户注册、登录
- JWT Token生成和验证
- 用户信息管理

### 2. 商品服务 (product-service)
- 商品列表查询
- 商品详情查询（支持Redis缓存）
- 库存扣减（乐观锁）
- 库存回滚

### 3. 订单服务 (order-service)
- 创建订单
- 订单查询
- 订单支付
- 订单取消

### 4. 秒杀服务 (seckill-service) ⭐核心
秒杀服务是本项目的核心，采用了多种高并发解决方案：

#### 高并发解决方案
1. **Redis库存预热**
   - 将数据库库存提前加载到Redis
   - 减少数据库访问压力
   - 提升响应速度

2. **Lua脚本原子操作**
   - 使用Lua脚本保证Redis操作的原子性
   - 防止并发问题
   - 避免超卖

3. **分布式锁（Redisson）**
   - 基于Redis的分布式锁
   - 保证同一时间只有一个请求扣减库存
   - 防止超卖问题

4. **令牌桶限流**
   - 控制每秒请求数量
   - 防止系统被流量击垮
   - 保护后端服务

5. **RabbitMQ异步削峰**
   - 秒杀请求异步处理
   - 削峰填谷
   - 提升系统吞吐量

6. **用户购买记录防重**
   - Redis记录用户购买记录
   - 防止用户重复购买
   - 保证业务公平性

7. **乐观锁**
   - 数据库层面使用乐观锁
   - 最后一道防线
   - 双重保障

## 快速开始

### 环境准备

#### 必需环境
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Docker & Docker Compose

#### 推荐IDE
- IntelliJ IDEA 2020+

### 1. 启动基础服务

使用Docker启动Redis和RabbitMQ：

```bash
# 进入项目根目录
cd ecommerce-seckill

# 启动Redis和RabbitMQ
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

服务访问地址：
- Redis: `localhost:6379`
- RabbitMQ管理界面: `http://localhost:15672` (用户名/密码: guest/guest)

### 2. 初始化数据库

```bash
# 使用MySQL客户端连接数据库
mysql -u root -p

# 执行初始化脚本
source db/init.sql

# 或者在MySQL Workbench中直接运行 db/init.sql 文件
```

**注意：** 请根据实际情况修改各服务的 `application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_seckill
    username: root
    password: your_password  # 修改为你的MySQL密码
```

### 3. 启动微服务

**方式一：使用IDEA启动（推荐开发环境）**

1. 打开IDEA，导入项目（File -> Open -> 选择项目根目录）
2. 等待Maven依赖下载完成
3. 按照以下顺序启动各个服务：

```
1. EurekaServerApplication (注册中心)
2. GatewayApplication (网关)
3. UserServiceApplication (用户服务)
4. ProductServiceApplication (商品服务)
5. OrderServiceApplication (订单服务)
6. SeckillServiceApplication (秒杀服务)
```

**方式二：使用Maven命令启动**

```bash
# 1. 打包项目
mvn clean package -DskipTests

# 2. 启动各个服务（在不同的终端窗口中执行）
cd eureka-server && mvn spring-boot:run
cd gateway && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd seckill-service && mvn spring-boot:run
```

### 4. 验证服务启动

访问Eureka注册中心查看服务注册情况：
```
http://localhost:8761
```

应该能看到以下服务已注册：
- GATEWAY
- USER-SERVICE
- PRODUCT-SERVICE
- ORDER-SERVICE
- SECKILL-SERVICE

## API测试

### 使用Postman或其他HTTP客户端测试

#### 1. 用户注册
```http
POST http://localhost:8080/user/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "email": "test@example.com",
  "nickname": "测试用户"
}
```

#### 2. 用户登录
```http
POST http://localhost:8080/user/login
Content-Type: application/json

{
  "username": "testuser1",
  "password": "password"
}
```

返回的token需要在后续请求中使用：
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJIUzUxMiJ9..."
}
```

#### 3. 查询商品列表
```http
GET http://localhost:8080/product/list?pageNum=1&pageSize=10
```

#### 4. 查询秒杀活动列表
```http
GET http://localhost:8080/seckill/active-list
```

#### 5. 秒杀库存预热（管理员操作）
```http
POST http://localhost:8080/seckill/warm-up/1
Authorization: {登录返回的token}
```

#### 6. 执行秒杀（核心接口）
```http
POST http://localhost:8080/seckill/do-seckill?activityId=1&quantity=1
Authorization: {登录返回的token}
```

#### 7. 查询订单列表
```http
GET http://localhost:8080/order/list?pageNum=1&pageSize=10
Authorization: {登录返回的token}
```

## 测试数据

数据库初始化脚本已包含测试数据：

### 测试账号
- 用户名：`testuser1` / `testuser2` / `admin`
- 密码：`password`（MD5加密后存储）

### 测试商品
- iPhone 15 Pro Max
- MacBook Pro 14 M3
- AirPods Pro 3
- iPad Air
- Apple Watch Ultra 2

### 秒杀活动
已预置3个进行中的秒杀活动，活动ID分别为1、2、3

## 高并发压测

### 使用JMeter进行压测

1. 下载Apache JMeter
2. 创建测试计划
3. 添加线程组（模拟用户）：
   - 线程数：1000
   - Ramp-Up时间：1秒
   - 循环次数：1

4. 添加HTTP请求：
   - 服务器：localhost
   - 端口：8080
   - 路径：/seckill/do-seckill?activityId=1&quantity=1
   - 方法：POST
   - 添加Header：Authorization: {token}

5. 观察结果：
   - 查看聚合报告
   - 检查Redis库存
   - 检查数据库订单数量
   - 验证是否有超卖

## 核心配置说明

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:              # 如果设置了密码请填写
    database: 0
    timeout: 10s
    lettuce:
      pool:
        max-active: 200    # 最大连接数
        max-wait: -1ms
        max-idle: 10
        min-idle: 0
```

### RabbitMQ配置
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual  # 手动确认
        prefetch: 1              # 每次拉取1条消息
        concurrency: 5           # 最小消费者数
        max-concurrency: 10      # 最大消费者数
```

### 数据库连接池配置
```yaml
spring:
  datasource:
    druid:
      initial-size: 5      # 初始连接数
      min-idle: 5          # 最小空闲连接
      max-active: 20       # 最大活动连接
      max-wait: 60000      # 最大等待时间
```

## 项目亮点

### 1. 完整的微服务架构
- 服务注册与发现
- API网关统一入口
- 服务间通信（Feign）
- 统一异常处理
- 统一响应格式

### 2. 高并发解决方案
- 多级缓存（Redis）
- 消息队列削峰（RabbitMQ）
- 分布式锁（Redisson）
- 限流（令牌桶）
- Lua脚本原子操作

### 3. 数据一致性保障
- 乐观锁
- 分布式锁
- 消息可靠性（手动ACK）
- 库存预热

### 4. 接口安全
- JWT Token认证
- 网关统一鉴权
- 防重提交

## 扩展计划

后续可以扩展接入以下技术：

### 1. ELK日志分析
- Elasticsearch：日志存储和搜索
- Logstash：日志收集
- Kibana：日志可视化

### 2. MongoDB
- 用户行为日志存储
- 商品浏览记录
- 秒杀活动热数据

### 3. Sentinel流控降级
- 更完善的限流策略
- 服务降级
- 熔断机制

### 4. Seata分布式事务
- 订单与库存的强一致性
- TCC模式
- AT模式

### 5. Nacos配置中心
- 统一配置管理
- 动态配置更新
- 配置版本管理

### 6. SkyWalking链路追踪
- 服务调用链追踪
- 性能分析
- 问题定位

## 常见问题

### 1. 服务启动失败
- 检查MySQL是否正常运行
- 检查Redis和RabbitMQ是否启动（docker-compose ps）
- 检查端口是否被占用
- 检查数据库连接信息是否正确

### 2. Eureka注册失败
- 确保Eureka Server先启动
- 检查网络连接
- 查看服务日志

### 3. Redis连接失败
- 检查Docker容器是否正常运行
- 检查端口映射是否正确
- 尝试使用redis-cli测试连接

### 4. RabbitMQ消息消费失败
- 查看RabbitMQ管理界面的队列状态
- 检查消费者是否正常启动
- 查看应用日志

### 5. 秒杀超卖问题
- 确保已执行库存预热操作
- 检查Lua脚本是否正确执行
- 验证分布式锁是否生效

## 项目结构说明

```
common/
├── result/              # 统一响应结果
├── exception/           # 异常处理
├── constant/            # 常量定义
└── utils/              # 工具类

seckill-service/
├── config/             # 配置类
│   ├── RedissonConfig      # Redisson分布式锁配置
│   └── RabbitMqConfig      # RabbitMQ队列配置
├── controller/         # 控制器
├── service/           # 业务逻辑
├── mapper/            # 数据访问
├── entity/            # 实体类
├── vo/                # 视图对象
└── mq/                # 消息队列消费者
```

## 性能优化建议

1. **Redis优化**
   - 使用连接池
   - 合理设置过期时间
   - 避免大key

2. **数据库优化**
   - 添加合适的索引
   - 使用连接池
   - 读写分离（可扩展）

3. **JVM优化**
   - 调整堆内存大小
   - 选择合适的垃圾回收器
   - 监控GC情况

4. **应用层优化**
   - 使用异步处理
   - 减少锁的粒度
   - 使用本地缓存

## 贡献指南

欢迎提交Issue和Pull Request！

## 许可证

MIT License

## 联系方式

如有问题，欢迎交流学习！

---

**祝你学习愉快，工作顺利！** 🚀



