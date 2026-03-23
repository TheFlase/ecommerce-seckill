# Docker网络和数据卷详解

## 🌐 1. Docker网络（Network）

### 什么是Docker网络？

**简单理解：** Docker网络就像是一个**虚拟的局域网**，让多个容器可以互相通信。

### 为什么需要创建网络？

#### 场景对比：

**没有网络（默认bridge网络）：**
```
容器A (Redis)    容器B (RabbitMQ)    容器C (你的应用)
    ↓                ↓                    ↓
    └────────────────┴────────────────────┘
             默认bridge网络
             
问题：所有容器都在同一个网络，容易冲突
```

**自定义网络（推荐）：**
```
┌─────────────────────────────────────┐
│    ecommerce-network (自定义网络)    │
│  ┌──────────┐  ┌──────────┐         │
│  │  Redis   │  │ RabbitMQ │         │
│  │ 容器     │  │ 容器     │         │
│  └──────────┘  └──────────┘         │
│     可以互相通信                      │
└─────────────────────────────────────┘
```

### 网络的作用

1. **容器间通信**
   - 容器可以通过容器名称互相访问
   - 例如：你的应用可以通过 `redis` 这个名称访问Redis容器

2. **网络隔离**
   - 不同项目的容器在不同网络，互不干扰
   - 提高安全性

3. **DNS自动解析**
   - Docker自动提供DNS服务
   - 容器名自动解析为IP地址

### 实际例子

```yaml
# docker-compose.yml
services:
  redis:
    container_name: ecommerce-redis
    networks:
      - ecommerce-network  # 加入这个网络
  
  rabbitmq:
    container_name: ecommerce-rabbitmq
    networks:
      - ecommerce-network  # 加入同一个网络
```

**效果：**
- Redis容器可以通过 `rabbitmq` 这个名称访问RabbitMQ
- RabbitMQ容器可以通过 `redis` 这个名称访问Redis
- 你的应用可以通过 `redis` 和 `rabbitmq` 访问它们

---

## 💾 2. 数据卷（Volume）

### 什么是数据卷？

**简单理解：** 数据卷是Docker提供的**持久化存储**，类似于U盘，数据不会因为容器删除而丢失。

### 为什么需要数据卷？

#### 问题场景：

**没有数据卷：**
```
启动Redis容器 → 写入数据 → 删除容器 → 数据丢失！❌
```

**有数据卷：**
```
启动Redis容器 → 写入数据到数据卷 → 删除容器 → 数据还在！✅
重新启动容器 → 数据卷挂载 → 数据恢复！
```

### 数据存储在哪里？

#### 关键点：数据存储在**宿主机**（你的电脑）上，不是容器内部！

```
┌─────────────────────────────────────────┐
│  宿主机（你的Windows/Mac/Linux电脑）     │
│  ┌───────────────────────────────────┐ │
│  │ Docker数据卷存储位置                │ │
│  │ Windows: C:\ProgramData\Docker\  │ │
│  │ Mac: /var/lib/docker/volumes/     │ │
│  │ Linux: /var/lib/docker/volumes/   │ │
│  └───────────────────────────────────┘ │
│           ↑                              │
│           │ 挂载（映射）                  │
│           │                              │
│  ┌───────────────────────────────────┐ │
│  │ Docker容器（Redis）                │ │
│  │ /data 目录 ← 映射到数据卷           │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### 数据卷的挂载方式

```yaml
volumes:
  - redis-data:/data  # 数据卷:容器内路径
```

**含义：**
- `redis-data`：数据卷名称（在宿主机上）
- `/data`：容器内的路径
- 容器内的 `/data` 目录实际存储在你的电脑上

---

## 📍 3. 不同操作系统数据卷位置

### Windows系统

#### Docker Desktop (WSL2后端)
```
位置：\\wsl$\docker-desktop-data\data\docker\volumes\
或者：C:\Users\<用户名>\AppData\Local\Docker\wsl\data\ext4.vhdx

查看命令：
docker volume inspect redis-data
```

**实际路径示例：**
```
\\wsl$\docker-desktop-data\data\docker\volumes\redis-data\_data
```

#### 查看数据卷位置的方法：

```powershell
# 1. 查看所有数据卷
docker volume ls

# 2. 查看数据卷详细信息（包括位置）
docker volume inspect redis-data

# 输出示例：
# {
#     "CreatedAt": "2024-12-26T...",
#     "Driver": "local",
#     "Mountpoint": "/var/lib/docker/volumes/redis-data/_data",
#     "Name": "redis-data",
#     ...
# }
```

### Mac系统

```
位置：/var/lib/docker/volumes/
或者：~/Library/Containers/com.docker.docker/Data/vms/0/data/docker/volumes/

查看命令：
docker volume inspect redis-data
```

### CentOS/Linux系统

```
位置：/var/lib/docker/volumes/

查看命令：
docker volume inspect redis-data
```

### 实际查看示例

```bash
# 查看redis-data数据卷的详细信息
docker volume inspect redis-data

# 输出：
{
    "CreatedAt": "2024-12-26T10:00:00Z",
    "Driver": "local",
    "Labels": {},
    "Mountpoint": "/var/lib/docker/volumes/redis-data/_data",
    "Name": "redis-data",
    "Options": {},
    "Scope": "local"
}
```

**Mountpoint** 就是数据实际存储的位置（在Docker内部路径，实际映射到宿主机）

---

## 🔌 4. RabbitMQ端口详解

### RabbitMQ的两个端口

```yaml
ports:
  - "5672:5672"   # AMQP协议端口（应用连接）
  - "15672:15672" # Web管理界面端口（浏览器访问）
```

### 端口1：5672（AMQP协议端口）

**作用：** 应用程序连接RabbitMQ的端口

**使用场景：**
- 你的Java应用通过这个端口连接RabbitMQ
- 发送和接收消息
- 这是RabbitMQ的核心功能端口

**连接示例：**
```java
// Spring Boot配置
spring:
  rabbitmq:
    host: localhost
    port: 5672  // ← 就是这个端口
    username: guest
    password: guest
```

### 端口2：15672（Web管理界面端口）

**作用：** RabbitMQ的Web管理界面

**使用场景：**
- 在浏览器中访问管理界面
- 查看队列、消息、连接状态
- 管理用户、权限等

**访问方式：**
```
浏览器打开：http://localhost:15672
用户名：guest
密码：guest
```

### 端口映射格式说明

```yaml
ports:
  - "宿主机端口:容器内端口"
```

**例子：**
```yaml
- "5672:5672"
  ↑     ↑
  │     └─ 容器内的端口（RabbitMQ默认端口）
  └─────── 宿主机（你的电脑）的端口
```

**含义：**
- 访问 `localhost:5672` → 转发到容器的5672端口
- 访问 `localhost:15672` → 转发到容器的15672端口

### 为什么需要端口映射？

```
┌─────────────────────────────────────────┐
│  宿主机（你的电脑）                      │
│  localhost:5672                        │
│       ↓                                │
│  Docker端口映射                         │
│       ↓                                │
│  ┌─────────────────────────────────┐  │
│  │ RabbitMQ容器                      │  │
│  │ 内部端口: 5672                    │  │
│  └─────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**原因：**
- 容器是隔离的环境
- 外部无法直接访问容器内部
- 通过端口映射，将宿主机的端口转发到容器端口

---

## 📊 完整示例解析

### docker-compose.yml完整解析

```yaml
version: '3.8'

services:
  redis:
    image: redis:7.0-alpine              # 使用Redis镜像
    container_name: ecommerce-redis       # 容器名称
    ports:
      - "6379:6379"                       # 端口映射：宿主机6379 → 容器6379
    volumes:
      - redis-data:/data                  # 数据卷：redis-data → 容器/data目录
    command: redis-server --appendonly yes # 启动命令：开启持久化
    networks:
      - ecommerce-network                 # 加入ecommerce-network网络
    restart: unless-stopped                # 自动重启策略

  rabbitmq:
    image: rabbitmq:3.11-management-alpine # RabbitMQ镜像（带管理界面）
    container_name: ecommerce-rabbitmq
    ports:
      - "5672:5672"                       # AMQP端口：应用连接
      - "15672:15672"                     # Web管理界面端口
    environment:
      RABBITMQ_DEFAULT_USER: guest        # 默认用户名
      RABBITMQ_DEFAULT_PASS: guest        # 默认密码
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq  # 数据卷：存储RabbitMQ数据
    networks:
      - ecommerce-network                 # 加入同一个网络
    restart: unless-stopped

volumes:
  redis-data:                             # 定义数据卷
    driver: local                         # 使用本地驱动
  rabbitmq-data:
    driver: local

networks:
  ecommerce-network:                      # 定义网络
    driver: bridge                        # 使用bridge驱动
```

---

## 🔍 实际操作验证

### 1. 查看网络

```bash
# 查看所有网络
docker network ls

# 查看网络详情
docker network inspect ecommerce-network

# 输出会显示哪些容器连接在这个网络上
```

### 2. 查看数据卷

```bash
# 查看所有数据卷
docker volume ls

# 查看数据卷详情（包括存储位置）
docker volume inspect redis-data
docker volume inspect rabbitmq-data
```

### 3. 查看端口映射

```bash
# 查看容器端口映射
docker port ecommerce-redis
docker port ecommerce-rabbitmq

# 或者查看容器详情
docker inspect ecommerce-rabbitmq | grep -A 10 "Ports"
```

### 4. 测试连接

```bash
# 测试Redis连接
docker exec -it ecommerce-redis redis-cli ping
# 应该返回：PONG

# 测试RabbitMQ管理界面
# 浏览器访问：http://localhost:15672
```

---

## 💡 常见问题

### Q1: 数据卷的数据会丢失吗？

**A:** 不会！数据卷是持久化存储，即使删除容器，数据卷依然存在。

**删除容器但保留数据卷：**
```bash
docker-compose down        # 删除容器，保留数据卷
docker-compose down -v    # 删除容器和数据卷（数据会丢失！）
```

### Q2: 可以自定义数据卷位置吗？

**A:** 可以！使用命名卷或绑定挂载：

```yaml
# 方式1：命名卷（Docker管理位置）
volumes:
  - redis-data:/data

# 方式2：绑定挂载（指定宿主机路径）
volumes:
  - /d/docker-data/redis:/data  # Windows: D:\docker-data\redis
  - ./redis-data:/data          # 相对路径
```

### Q3: 为什么需要两个网络？

**A:** 通常不需要两个网络。一个项目一个网络即可，用于隔离不同项目。

### Q4: 端口冲突怎么办？

**A:** 修改宿主机端口：

```yaml
ports:
  - "16379:6379"    # 如果6379被占用，改用16379
  - "25672:5672"    # 如果5672被占用，改用25672
```

### Q5: 容器间如何通信？

**A:** 通过容器名称：

```java
// 在同一个网络中，可以直接使用容器名
spring:
  rabbitmq:
    host: ecommerce-rabbitmq  # 使用容器名，不是localhost
    port: 5672
```

---

## 📝 总结

### 网络（Network）
- **作用**：让容器可以互相通信
- **类型**：bridge网络（桥接网络）
- **好处**：DNS自动解析、网络隔离

### 数据卷（Volume）
- **作用**：持久化存储数据
- **位置**：存储在宿主机上
- **好处**：数据不丢失，可以共享

### 端口映射
- **格式**：`宿主机端口:容器端口`
- **作用**：让外部可以访问容器内的服务
- **RabbitMQ**：
  - 5672：应用连接端口
  - 15672：Web管理界面端口

### 数据存储位置
- **Windows**：`\\wsl$\docker-desktop-data\data\docker\volumes\`
- **Mac/Linux**：`/var/lib/docker/volumes/`
- **查看方法**：`docker volume inspect <卷名>`

---

**希望这个解释更清楚了！** 🎯



