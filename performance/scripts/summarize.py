#!/usr/bin/env python3
"""Build a markdown performance report from k6 summary and EXPLAIN captures."""
from __future__ import annotations

import json
import os
import pathlib
import sys


def metric(summary: dict, name: str, field: str) -> str:
    values = summary.get("metrics", {}).get(name, {}).get("values", {})
    if field not in values:
        return "未采集"
    value = values[field]
    if isinstance(value, float):
        return f"{value:.2f}"
    return str(value)


def read_text(path: pathlib.Path) -> str:
    if not path.exists():
        return "未采集"
    text = path.read_text(encoding="utf-8", errors="replace")
    if len(text) > 12000:
        return text[:12000] + "\n...[truncated]...\n"
    return text


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: summarize.py <k6-summary.json|NONE> <out.md>", file=sys.stderr)
        return 2
    summary_arg, out_path = sys.argv[1], pathlib.Path(sys.argv[2])
    out_dir = pathlib.Path(os.environ.get("P1_OUT_DIR", out_path.parent))
    hardware = read_text(out_dir / "hardware.txt")
    explain_before = read_text(out_dir / "explain-before.txt")
    explain_after = read_text(out_dir / "explain-after.txt")
    reconcile = read_text(out_dir / "reconcile.txt")

    summary = {}
    k6_section = "本次未执行 k6（仅 EXPLAIN / 对账）。"
    thresholds_ok = "未执行"
    if summary_arg != "NONE" and pathlib.Path(summary_arg).exists():
        summary = json.loads(pathlib.Path(summary_arg).read_text(encoding="utf-8"))
        catalog_p95 = metric(summary, "catalog_read_ms", "p(95)")
        order_p95 = metric(summary, "order_create_ms", "p(95)")
        business_rate = metric(summary, "business_ok", "rate")
        http_fail = metric(summary, "http_req_failed", "rate")
        orders = metric(summary, "orders_created", "count")
        duration = metric(summary, "http_req_duration", "p(95)")
        k6_section = f"""
| 指标 | 目标 | 结果 |
|---|---|---|
| 目录读 p95 (`catalog_read_ms`) | < 500ms | {catalog_p95} ms |
| 下单 p95 (`order_create_ms`) | < 1000ms | {order_p95} ms |
| 业务成功比率 (`business_ok`) | > 99% | {business_rate} |
| HTTP 失败率 | 参考 | {http_fail} |
| 成功下单计数 | 记录 | {orders} |
| 全请求 p95 (`http_req_duration`) | 参考 | {duration} ms |
"""
        try:
            catalog_ok = float(catalog_p95) < 500
            order_ok = float(order_p95) < 1000
            biz_ok = float(business_rate) > 0.99
            thresholds_ok = "达标" if catalog_ok and order_ok and biz_ok else "未全部达标（保留真实结果，未删除失败数据）"
        except ValueError:
            thresholds_ok = "指标无法解析"

    report = f"""# P1 性能报告

- 数据规模：20 个 P1 分类、1200 个目录 SKU、热销 OTC 500000 可售、100 个用户、20000 条历史订单。
- k6 脚本：`performance/k6/catalog_and_order.js`（100 VU / 10 min，约 75% 读、15% 订单列表、10% 下单）。
- 阈值结论：**{thresholds_ok}**

## 硬件与软件

```
{hardware}
```

## k6 结果

{k6_section}

## 对账

```
{reconcile}
```

## EXPLAIN ANALYZE（V4 后、V5 前）

```
{explain_before}
```

## EXPLAIN ANALYZE（V5 后）

```
{explain_after}
```

## 说明

- 本报告数字只来自同一次 GitHub Actions 产物，不是本地估计。
- 未达标指标不得改写。k6 与应用、MySQL 同机运行时，p95 可能受 CPU 争用影响。
"""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(report, encoding="utf-8")
    print(f"wrote {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
