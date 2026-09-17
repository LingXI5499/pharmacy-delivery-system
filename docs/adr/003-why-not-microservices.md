# ADR 003：为何不拆微服务

状态：已采纳

日期：2026-09

## 问题

简历常见写法是「Spring Cloud 微服务药房系统」。面试官会问：拆分标准、分布式事务、如何证明库存正确。

## 约束

- 单店即时配送，没有多团队边界
- 必须能演示 FEFO 与支付幂等，而不是演示注册中心
- AGENTS.md 禁止引入 Spring Cloud / Docker / K8s / ES

## 方案

保持模块化单体。需要隔离的是**正确性边界**（DB 约束、行锁、唯一键），不是进程边界。缓存、限流、消息都是可降级附件。

## 失败方案

为作品集搭 Nacos + Gateway + 四个 jar。拒绝原因：无法在有限时间内做真实对账；容易把脚手架当成生产经验。

## 测试 / 结果

- 架构差距写在 [architecture.md](../architecture.md)：不替代实习、JVM 与网络基础
- 性能证据来自单进程 + 原生 MySQL 的 k6，见 [reports/p1-performance.md](../reports/p1-performance.md)
