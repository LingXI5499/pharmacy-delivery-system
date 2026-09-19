# 状态机

订单主路径见 [architecture.md](architecture.md)。此处补采购与盘点，以及订单取消/退款边界。

## 订单

```mermaid
stateDiagram-v2
  [*] --> PENDING_REVIEW: 含处方药
  [*] --> PENDING_PAYMENT: OTC 且 FEFO 预占成功
  PENDING_REVIEW --> PENDING_PAYMENT: 药师通过且预占成功
  PENDING_REVIEW --> REVIEW_REJECTED: 药师拒绝
  PENDING_REVIEW --> CLOSED_STOCK_SHORTAGE: 通过但无可售批次
  PENDING_REVIEW --> CANCELED: 用户/管理员取消并释放
  PENDING_PAYMENT --> PENDING_ACCEPT: 模拟支付成功并确认出库
  PENDING_PAYMENT --> CLOSED_TIMEOUT: 30 分钟未支付
  PENDING_PAYMENT --> CANCELED: 支付前取消并释放
  PENDING_ACCEPT --> TO_PACK: 接单
  PENDING_ACCEPT --> CANCELED: 取消
  TO_PACK --> TO_DISPATCH: 打包
  TO_PACK --> CANCELED: 管理员取消走退款
  TO_DISPATCH --> DELIVERING: 派可用骑手
  TO_DISPATCH --> CANCELED: 管理员取消走退款
  DELIVERING --> COMPLETED: 确认送达
```

退款：发货前状态才回补库存；配送中/已完成不得错误回补。以 `RefundService` 的 `restockRequired` 为准。

## 采购单

```mermaid
stateDiagram-v2
  [*] --> DRAFT: 采购员创建
  DRAFT --> APPROVED: 管理员批准
  DRAFT --> REJECTED: 管理员拒绝且必填原因
  APPROVED --> PARTIALLY_RECEIVED: 部分收货
  APPROVED --> RECEIVED: 一次收齐
  PARTIALLY_RECEIVED --> RECEIVED: 收齐
```

批准不入库。合格收货才增加可售批次与 `PURCHASE_RECEIPT` 流水。

## 盘点单

```mermaid
stateDiagram-v2
  [*] --> DRAFT
  DRAFT --> COUNTING: 开始
  DRAFT --> CANCELED: 取消
  COUNTING --> COMPLETED: 完成（仅一次）
  COUNTING --> CANCELED: 取消
```
