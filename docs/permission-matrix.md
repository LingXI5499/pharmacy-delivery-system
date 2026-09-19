# 权限矩阵

两道门：**路径角色**（`SecurityConfig`）+ **权限码**（`@PreAuthorize @permissionService.has`）。

| 路径前缀 | 角色 | 权限码（若有） | 说明 |
|---|---|---|---|
| `/api/auth/login|register|refresh` | 匿名 | — | 登录限流 20/分/IP |
| `/api/public/**` | 匿名 | — | 分类与上架药品 |
| `/api/user/**` | 已登录 | — | 本人购物车/地址/订单/模拟支付 |
| `/api/admin/**` | ADMIN | 各接口再查权限 | 后台、采购审批、履约 |
| `/api/pharmacist/**` | PHARMACIST, ADMIN | `prescription.review` | 待审处方 |
| `/api/purchaser/**` | PURCHASER, ADMIN | `procurement.write` | 供应商与采购单 |
| `/api/warehouse/**` | WAREHOUSE, ADMIN | `inventory.read` / `inventory.write` | 收货、批次、盘点 |

| 权限码 | ADMIN | PHARMACIST | PURCHASER | WAREHOUSE | USER |
|---|---|---|---|---|---|
| prescription.review | 是 | 是 | 否 | 否 | 否 |
| procurement.write | 是 | 否 | 是 | 否 | 否 |
| inventory.read | 是 | 否 | 否 | 是 | 否 |
| inventory.write | 是 | 否 | 否 | 是 | 否 |
| order.fulfill | 是 | 否 | 否 | 否 | 否 |
| audit.read | 是 | 否 | 否 | 否 | 否 |

越权原则：未登录 401；角色不够 403；资源不属于你时用 404 或业务拒绝，不暴露「资源存在」。

当前演示库只有 `admin`（ADMIN）和 `user01`（USER）。管理员可打开 `/purchaser`、`/warehouse`、`/pharmacist`。没有单独的药师/采购/仓库登录账号，除非管理员在用户管理里改角色。
