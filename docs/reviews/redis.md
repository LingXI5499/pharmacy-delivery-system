# 复盘：Redis 只做可降级加速

## 问题

限流、目录缓存、JWT deny list 需要低延迟。若把幂等或库存余额放进 Redis，主从切换或 flush 会直接做错账。

## 约束

- MySQL 约束和事务守最终正确性
- Redis 停机不得阻止下单提交

## 方案

- 登录/下单限流：`RateLimitFilter` 用 Redis 计数；`Redis` 抛错时 **放行**（catch RuntimeException）
- 目录 `@Cacheable`；压测用 `SPRING_CACHE_TYPE=simple` 避免把缓存命中写成 SQL 已优化
- JWT 登出 deny list 在 Redis；丢失时仍靠 Refresh 族撤销

## 失败方案

「先 incr Redis 再扣库存」。拒绝：计数器不是账本。

## 测试 / 结果

- 代码：`RateLimitFilter` 捕获 `RuntimeException` 后继续过滤链，不阻断下单
- Q1 安全测试覆盖 refresh 轮换（本基线已合入）
- Q3 PR [#11](https://github.com/LingXI5499/pharmacy-delivery-system/pull/11) 才补限流失败单测；未合并前不把该测试当成本基线证据
- 诊断步骤：[ops/fault-diagnosis.md](../ops/fault-diagnosis.md)
- 本复盘不是一次真实生产事故记录
