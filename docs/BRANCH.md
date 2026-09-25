# 分支与版本说明

## 分支对照

| 引用 | 说明 |
|------|------|
| `main` / tag `v1-boot2-eureka` | 旧版：Spring Boot **2.6** + **Eureka** + 固定窗口限流 |
| `feature/boot3-nacos-modernization` | 新版：Spring Boot **3.2** + **Nacos** + **Sentinel** + 生产向加固 |

## 技术栈差异（目标态）

| 能力 | v1（Boot2） | 本分支（现代化） |
|------|-------------|------------------|
| JDK | 8/11 可跑 | **17 必需** |
| 注册中心 | Eureka `:8761` | Nacos `:8848` |
| 限流 | Redis 固定窗口 | Sentinel Dashboard `:8858` |
| 密码 | MD5 | BCrypt |
| 消息 | 普通队列 | 业务队列 + **死信队列** |
| 一致性 | Redis 预扣 + MQ 落库 | 同上 + **定时对账** |
| 基建 | Redis/RabbitMQ | MySQL+Redis+RabbitMQ+Nacos+Sentinel 全 Docker |

## 本地端口

| 组件 | 端口 |
|------|------|
| MySQL（Docker） | **3307**（避免与本机 3306 冲突） |
| Redis | 6379 |
| RabbitMQ / 管理台 | 5672 / 15672 |
| Nacos | 8848 |
| Sentinel Dashboard | 8858 |
| Gateway | 8080 |
| user / product / order / seckill | 8081–8084 |

## 演示账号

- `testuser1` / `password`
- `testuser2` / `password`
- `admin` / `password`（预热等管理接口）

## 合并策略

本分支验收通过后可通过 PR 合并进 `main`。旧版可通过 tag `v1-boot2-eureka` 随时检出对照。
