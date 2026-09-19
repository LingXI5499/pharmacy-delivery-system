# 复盘：RabbitMQ 与订单事务解耦

## 问题

超时关单需要延迟消息。若 `convertAndSend` 放在 `@Transactional` 内等待 Broker，RabbitMQ 宕机会回滚已经合法的预占与订单。

## 约束

AGENTS.md：RabbitMQ 不可用不能回滚已经正确提交的核心订单；事件必须保留并在恢复后安全重放。

## 方案

Outbox（Modulith JDBC）→ 提交后投递 → Confirm → Inbox 去重 → Ack。未完成事件可重放归档。

P1 为测 HTTP p95 关闭了 `MESSAGING_ENABLED`。那次 32ms 下单 **不包含** Publisher Confirm。

## 失败方案

仅 Redis TTL 做关单。拒绝：进程重启或 Redis 丢键会漏关或错关。

## 测试 / 结果

- 消息层单测：超时短路、Inbox 去重、重放失败不炸主流程
- 告警：`pharmacy_outstanding_events` 见 [ops/fault-diagnosis.md](../ops/fault-diagnosis.md)
- 未在 D1 对真实 Broker 再做一次宕机演练；O1 文档描述了预期行为
