# P1 性能工具

隔离库与虚构 SKU/账号，不连接开发库。

```bash
export DB_URL=jdbc:mysql://127.0.0.1:3306/pharmacy_delivery_perf?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
export DB_USERNAME=...
export DB_PASSWORD=...
export JWT_SECRET=local-performance-secret-change-me-32bytes
export P1_DB_NAME=pharmacy_delivery_perf
export P1_DB_USER=...
export P1_DB_PASSWORD=...
bash performance/scripts/run-perf.sh explain   # EXPLAIN + 对账
bash performance/scripts/run-perf.sh full      # 再跑 100 VU / 10 min
```

GitHub：Actions → V2 performance → Run workflow。不要把本目录的 10 分钟压测加进 `ci.yml`。
