# ADR 002：可靠领域事件

状态：已采纳

日期：2026-09

## 问题

支付超时关单、库存释放需要异步。若把「发到 RabbitMQ」放在业务事务里，Broker 宕机会回滚已经正确的订单；若先提交再裸发消息，会丢事件。

## 约束

- RabbitMQ 不可用不能回滚已提交核心订单
- 恢复后必须可安全重放，不能重复释放库存
- Redis 不能当事件唯一真相

## 方案

1. Spring Modulith JDBC 与业务同事务登记事件（Outbox）。
2. 提交后再投递，Publisher Confirm；Nack/超时不标记完成。
3. 消费者先写 Inbox `(consumer_name, event_id)` 唯一键，再处理业务，提交后 Ack。
4. 定时重放未完成事件。

参考 mall-swarm 的 TTL 只学「到期检查」，不把它当完整 Outbox。许可证见 [reference-audit.md](../reference-audit.md)。

## 失败方案

- 只在 Redis 里记「已处理」：拒绝，Redis 可丢。
- 事务里 `convertAndSend` 并等待 Broker：拒绝，会把 MQ 故障耦合进下单。

## 测试 / 结果

- 消息相关单测覆盖 Inbox 去重、超时关单短路、重放失败不抛崩
- P1 压测关闭了 `MESSAGING_ENABLED`，**不能**把那次 p95 说成生产消息路径成绩
- Broker 宕机时的「订单已提交、事件待重放」以代码与单测为准；完整故障演练日志见 O1 文档，未在 D1 重跑
