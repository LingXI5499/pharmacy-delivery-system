# OpenAPI 使用说明

后端 SpringDoc：

- UI：`http://localhost:8089/swagger-ui.html`
- 文档：`http://localhost:8089/v3/api-docs`

生产应关闭或加鉴权：`springdoc.api-docs.enabled` / `springdoc.swagger-ui.enabled`。

## 怎么用

1. 启动后端（演示可 `MESSAGING_ENABLED=false`）。
2. 用 `/api/auth/login` 拿 `accessToken`。
3. Swagger 点 Authorize，填 `Bearer <token>`。
4. 写操作看角色：USER 走 `/api/user/**`，ADMIN 走 `/api/admin/**`。

## 稳定约定

| 项 | 约定 |
|---|---|
| 包体 | `{ code, message, data, timestamp }`，`code=0` 成功 |
| 下单 | Header `Idempotency-Key` 1～80 字符 |
| 模拟支付 | `/api/user/orders/{id}/payments` 再 `/api/user/mock-payments/callback` |
| 处方上传 | multipart，仅 PDF/JPEG/PNG 真实内容 |
| 错误 | 401 未登录；403 角色不够；业务冲突用项目 `ErrorCode`，不是随便 500 |

前端通过 Vite 代理 `/api` → `8089`。本机若用 5174，后端 CORS 需包含该 Origin（Q3 PR 已改 allowlist；未合并前请用 5173 或自行配置）。

OpenAPI 描述的是教学 API，不是对外商业契约。
