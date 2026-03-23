# API接口文档

## 基础信息

- **基础URL**: `http://localhost:8080`
- **认证方式**: JWT Token (部分接口需要)
- **请求格式**: JSON
- **响应格式**: JSON

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 状态码说明
- 200: 成功
- 500: 业务异常或系统异常
- 401: 未授权（Token无效或过期）

---

## 用户服务 (User Service)

### 1. 用户注册

**接口地址**: `POST /user/register`

**是否需要认证**: 否

**请求参数**:
```json
{
  "username": "testuser",
  "password": "123456",
  "phone": "13800138000",
  "email": "test@example.com",
  "nickname": "测试用户"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 2. 用户登录

**接口地址**: `POST /user/login`

**是否需要认证**: 否

**请求参数**:
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIiLCJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcwMzA2MDgwMCwiZXhwIjoxNzAzMTQ3MjAwfQ.xxx"
}
```

**说明**: 返回的token需要在后续需要认证的接口中，通过Header `Authorization` 传递

### 3. 获取用户信息

**接口地址**: `GET /user/info/{userId}`

**是否需要认证**: 是

**请求参数**: 
- Path参数: userId (用户ID)

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "phone": "13800138000",
    "email": "test@example.com",
    "nickname": "测试用户",
    "status": 0,
    "createTime": "2024-12-20 10:00:00",
    "updateTime": "2024-12-20 10:00:00"
  }
}
```

---

## 商品服务 (Product Service)

### 1. 商品列表查询

**接口地址**: `GET /product/list`

**是否需要认证**: 否

**请求参数**:
- pageNum: 页码（默认1）
- pageSize: 每页数量（默认10）
- category: 分类（可选）

**示例**: `GET /product/list?pageNum=1&pageSize=10&category=手机数码`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "productName": "iPhone 15 Pro Max 256GB",
        "description": "最新款苹果手机，A17 Pro芯片，钛金属设计",
        "imageUrl": "https://example.com/iphone15.jpg",
        "originalPrice": 9999.00,
        "currentPrice": 8999.00,
        "stock": 1000,
        "sales": 50,
        "category": "手机数码",
        "status": 1
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 2. 商品详情查询

**接口地址**: `GET /product/detail/{productId}`

**是否需要认证**: 否

**请求参数**:
- Path参数: productId (商品ID)

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "productName": "iPhone 15 Pro Max 256GB",
    "description": "最新款苹果手机，A17 Pro芯片，钛金属设计",
    "imageUrl": "https://example.com/iphone15.jpg",
    "originalPrice": 9999.00,
    "currentPrice": 8999.00,
    "stock": 1000,
    "sales": 50,
    "category": "手机数码",
    "status": 1,
    "createTime": "2024-12-20 10:00:00",
    "updateTime": "2024-12-20 10:00:00"
  }
}
```

---

## 订单服务 (Order Service)

### 1. 创建订单

**接口地址**: `POST /order/create`

**是否需要认证**: 是

**请求头**:
```
Authorization: {token}
```

**请求参数**:
```json
{
  "productId": 1,
  "productName": "iPhone 15 Pro Max 256GB",
  "quantity": 1,
  "price": 8999.00
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "1234567890123456789",
    "userId": 1,
    "productId": 1,
    "productName": "iPhone 15 Pro Max 256GB",
    "quantity": 1,
    "price": 8999.00,
    "totalPrice": 8999.00,
    "status": 0,
    "orderType": 0,
    "createTime": "2024-12-20 10:00:00"
  }
}
```

### 2. 查询订单列表

**接口地址**: `GET /order/list`

**是否需要认证**: 是

**请求参数**:
- pageNum: 页码（默认1）
- pageSize: 每页数量（默认10）

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "orderNo": "1234567890123456789",
        "userId": 1,
        "productId": 1,
        "productName": "iPhone 15 Pro Max 256GB",
        "quantity": 1,
        "price": 8999.00,
        "totalPrice": 8999.00,
        "status": 0,
        "orderType": 0,
        "createTime": "2024-12-20 10:00:00"
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 3. 查询订单详情

**接口地址**: `GET /order/detail/{orderNo}`

**是否需要认证**: 是

**请求参数**:
- Path参数: orderNo (订单号)

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "1234567890123456789",
    "userId": 1,
    "productId": 1,
    "productName": "iPhone 15 Pro Max 256GB",
    "quantity": 1,
    "price": 8999.00,
    "totalPrice": 8999.00,
    "status": 0,
    "orderType": 0,
    "createTime": "2024-12-20 10:00:00",
    "updateTime": "2024-12-20 10:00:00"
  }
}
```

### 4. 取消订单

**接口地址**: `POST /order/cancel/{orderNo}`

**是否需要认证**: 是

**请求参数**:
- Path参数: orderNo (订单号)

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5. 支付订单

**接口地址**: `POST /order/pay/{orderNo}`

**是否需要认证**: 是

**请求参数**:
- Path参数: orderNo (订单号)

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 秒杀服务 (Seckill Service) ⭐核心

### 1. 查询进行中的秒杀活动列表

**接口地址**: `GET /seckill/active-list`

**是否需要认证**: 否

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "activityName": "双12手机秒杀",
      "productId": 1,
      "productName": "iPhone 15 Pro Max 256GB",
      "originalPrice": 9999.00,
      "seckillPrice": 6999.00,
      "seckillStock": 100,
      "soldCount": 20,
      "limitPerUser": 1,
      "startTime": "2024-12-20 10:00:00",
      "endTime": "2024-12-20 22:00:00",
      "status": 1
    }
  ]
}
```

### 2. 查询秒杀活动详情

**接口地址**: `GET /seckill/detail/{activityId}`

**是否需要认证**: 否

**请求参数**:
- Path参数: activityId (活动ID)

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "activityName": "双12手机秒杀",
    "productId": 1,
    "productName": "iPhone 15 Pro Max 256GB",
    "originalPrice": 9999.00,
    "seckillPrice": 6999.00,
    "seckillStock": 100,
    "soldCount": 20,
    "limitPerUser": 1,
    "startTime": "2024-12-20 10:00:00",
    "endTime": "2024-12-20 22:00:00",
    "status": 1,
    "createTime": "2024-12-19 10:00:00",
    "updateTime": "2024-12-20 10:00:00"
  }
}
```

### 3. 秒杀库存预热 ⚙️

**接口地址**: `POST /seckill/warm-up/{activityId}`

**是否需要认证**: 是（建议管理员接口）

**请求参数**:
- Path参数: activityId (活动ID)

**请求头**:
```
Authorization: {token}
```

**说明**: 在秒杀开始前，需要调用此接口将库存加载到Redis

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 4. 执行秒杀 ⭐⭐⭐

**接口地址**: `POST /seckill/do-seckill`

**是否需要认证**: 是

**请求参数**:
- activityId: 活动ID
- quantity: 购买数量

**示例**: `POST /seckill/do-seckill?activityId=1&quantity=1`

**请求头**:
```
Authorization: {token}
```

**响应示例（成功）**:
```json
{
  "code": 200,
  "message": "success",
  "data": "1734678901234567890"
}
```
注：data字段返回的是订单号

**响应示例（失败）**:
```json
{
  "code": 500,
  "message": "商品已抢光",
  "data": null
}
```

**可能的错误信息**:
- "秒杀活动未开始或已结束"
- "秒杀活动未开始"
- "秒杀活动已结束"
- "超过限购数量"
- "您已经参与过该秒杀活动"
- "系统繁忙，请稍后重试"（限流）
- "商品已抢光"

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未授权（Token无效或过期） |
| 500 | 业务异常或系统异常 |

## 注意事项

1. **Token使用**
   - Token通过登录接口获取
   - Token有效期为24小时
   - 需要在请求头中添加: `Authorization: {token}`

2. **秒杀流程**
   - 管理员需要先调用库存预热接口
   - 用户登录获取token
   - 用户调用秒杀接口
   - 秒杀成功后会异步创建订单

3. **限流策略**
   - 每个活动每秒最多处理1000个请求
   - 超过限制会返回"系统繁忙"错误

4. **防重机制**
   - 每个用户对同一活动只能秒杀一次
   - Redis记录24小时

5. **分页参数**
   - pageNum: 从1开始
   - pageSize: 建议不超过100

## 测试建议

1. **单接口测试**
   - 使用Postman或其他HTTP客户端
   - 按照文档示例调用

2. **压力测试**
   - 使用JMeter或Gatling
   - 模拟1000+并发用户
   - 验证是否有超卖

3. **业务流程测试**
   - 用户注册 -> 登录 -> 查看活动 -> 秒杀 -> 查看订单

---

**更新时间**: 2024-12-26



