# 指标与仪表盘

## 采集

- 应用暴露：`/actuator/prometheus`（需认证；公网 Nginx deny）
- Prometheus：`deploy/prometheus/prometheus.yml` 仅 scrape `127.0.0.1:8089`
- systemd 单元示例：`deploy/prometheus/pharmacy-prometheus.service`

## 业务指标

| 指标 | 含义 |
|---|---|
| `pharmacy_order_business_failures_total` | 订单/状态冲突等业务失败 |
| `pharmacy_inventory_conflicts_total` | 库存冲突（40901） |
| `pharmacy_auth_failures_total` | 401/403 |
| `pharmacy_outstanding_events` | `EVENT_PUBLICATION` 未完成行数 |

## Grafana

1. 安装 Grafana（Linux 原生包，不用 Docker）
2. 使用 `deploy/grafana/provisioning/**`
3. 仪表盘 JSON：`deploy/grafana/dashboards/pharmacy-overview.json`
