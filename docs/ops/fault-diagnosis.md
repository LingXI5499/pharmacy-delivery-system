# Redis / RabbitMQ / MySQL 故障诊断与恢复标准

## 告警阈值（与 `deploy/prometheus/pharmacy-alerts.yml` 对齐）

| 信号 | 阈值 | 严重级别 | 恢复标准 |
|---|---|---|---|
| Prometheus `up` | `==0` 持续 1m | critical | 进程与 scrape 恢复，`up=1` |
| HTTP 5xx rate | `>0.2/s` 持续 5m | warning | 5xx 回落，错误日志可按 traceId 闭环 |
| 库存冲突 | 10m 内 `>20` | warning | 冲突增速下降，库存不为负 |
| 未完成事件 | `pharmacy_outstanding_events > 50` 持续 10m | warning | 事件数回落，RabbitMQ 可连通 |
| JVM heap | `>85%` 持续 10m | warning | heap 回落或完成有计划重启 |

## Redis

诊断：

```bash
redis-cli -h 127.0.0.1 ping
redis-cli info memory
```

影响：缓存/限流/JWT deny list；核心下单正确性不依赖 Redis。  
恢复：重启 Redis → 观察缓存回填与登录/登出；deny list 丢失时依赖 refresh 轮换仍可撤销会话族。

## RabbitMQ

诊断：

```bash
sudo rabbitmqctl status
sudo rabbitmqctl list_queues name messages consumers
```

影响：超时关单与领域事件投递；核心订单事务在 Broker 宕机时仍应提交并保留未完成事件。  
恢复：Broker 恢复 → 确认 publisher confirm / 定时重放 → `pharmacy_outstanding_events` 下降。

## MySQL

诊断：

```bash
mysqladmin -h127.0.0.1 -upharmacy_app -p status
mysql -e "SHOW PROCESSLIST; SHOW ENGINE INNODB STATUS\G"
```

恢复：优先只读排查 → 必要时切换只读/限流 → 使用临时库备份恢复演练；**禁止**在未确认目标上执行 restore/drop。

## 仪表盘

导入 `deploy/grafana/dashboards/pharmacy-overview.json`，覆盖 JVM、HTTP、连接池、订单失败、未完成事件、库存冲突。
