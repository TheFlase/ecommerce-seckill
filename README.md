# 电商秒杀微服务（Boot3 + Nacos + Sentinel）

现代化改造分支：`feature/boot3-nacos-modernization`  
旧版对照：tag `v1-boot2-eureka`（Boot2 + Eureka）  
详见 [docs/BRANCH.md](docs/BRANCH.md)

## 技术栈

- JDK **17** / Spring Boot **3.2** / Spring Cloud **2023.0**
- Nacos 注册发现、Sentinel 限流
- MySQL + Redis + RabbitMQ（含死信队列）
- Redisson 锁、BCrypt、库存对账任务

## 前置

1. JDK 17
2. Maven 3.8+
3. Docker Desktop

## 快速启动

```powershell
# 1) 基础设施（MySQL 映射本机 3307）
.\scripts\start-infra.ps1

# 2) 编译
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
mvn -DskipTests clean install

# 3) 启动业务服务（后台日志在 logs\）
.\scripts\run-service.ps1 -Module gateway -Name gateway
.\scripts\run-service.ps1 -Module user-service -Name user
.\scripts\run-service.ps1 -Module product-service -Name product
.\scripts\run-service.ps1 -Module order-service -Name order
.\scripts\run-service.ps1 -Module seckill-service -Name seckill

# 4) 冒烟（login → admin 预热 → 秒杀）
.\scripts\demo-smoke.ps1

# 5) 压测（默认 50 用户 / 并发 20；每次自动注册新用户）
.\scripts\load-seckill.ps1
.\scripts\load-seckill.ps1 -Users 80 -Concurrency 40
```

## 端口

| 组件 | 地址 |
|------|------|
| Gateway | http://localhost:8080 |
| Nacos | http://localhost:8848/nacos |
| Sentinel | http://localhost:8858 （sentinel/sentinel） |
| RabbitMQ | http://localhost:15672 （guest/guest） |
| MySQL | localhost:**3307** root/123456 |

## 账号

`testuser1` / `testuser2` / `admin`，密码均为 `password`  
预热接口 `/seckill/warm-up/**` 仅 `admin`。

## 高并发方案（真实能力）

1. Redis 库存预热 + Lua 原子扣减  
2. 用户 SET NX 防重  
3. Sentinel 限流（`doSeckill`）  
4. RabbitMQ 异步落库 + DLQ  
5. 消费者 Redisson + DB 乐观锁；失败回补 Redis  
6. 定时 Redis/DB 库存对账告警  

不上 Seata：秒杀采用最终一致性。
