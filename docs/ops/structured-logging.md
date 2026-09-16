# 结构化日志约定

## 字段

每个请求日志应稳定出现：

| MDC / 字段 | 来源 | 说明 |
|---|---|---|
| `traceId` | `TraceIdFilter` / `X-Trace-Id` | 贯穿一次 HTTP 请求 |
| `userId` | `JwtAuthenticationFilter` | 未登录为空 |
| `businessId` | 默认 `METHOD URI`；业务代码可覆盖为订单号等 | 禁止写入令牌或处方内容 |
| `errorCode` | `GlobalExceptionHandler` | 业务/系统错误码 |

Logback pattern 见 `backend/src/main/resources/logback-spring.xml`。

## 脱敏

`RequestLogContext.sanitize` 会拦截含 `bearer`、`refresh`、`password`、`authorization` 以及疑似处方路径的值。

## 从一次请求定位订单 / 库存 / 事件

1. 从响应头或日志取出 `traceId`。
2. 查 `business_audit_log`：`SELECT * FROM business_audit_log WHERE trace_id=?`。
3. 若 `businessId` 含订单路径或业务代码写入了 `orderNo`，再查：
   - `pharmacy_order` / `payment_attempt` / `refund_record`
   - `inventory_ledger`（`business_id`）
   - `EVENT_PUBLICATION`（未完成事件）与 `inbox_event`
4. Grafana 面板 `Outstanding Events` / `Inventory Conflicts` 用于确认是否积压。

禁止在日志中输出 JWT、Refresh Token、处方文件字节或真实患者信息（本项目仅使用虚构数据）。
