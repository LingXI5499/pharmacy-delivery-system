# 速安药房项目 GitHub 发布与 Git 推送指南

## 1. 推荐的 GitHub 仓库信息

### Repository name

```text
pharmacy-delivery-system
```

最推荐 `pharmacy-delivery-system`，简洁、专业、语义明确。

### Description

```text
基于 Spring Boot 3 + Vue 3 + MyBatis-Plus + MySQL 的单店药店即时配送管理系统，包含用户端、后台管理端、购物车、库存、订单状态机、骑手配送与数据看板。
```

### Topics

```text
java
spring-boot
vue3
typescript
mybatis-plus
mysql
vite
pinia
element-plus
echarts
full-stack
pharmacy
delivery
```

### Public 还是 Private

如果主要目的是秋招作品集和星雨笔录展示，建议设为 **Public**。

但公开前必须确认仓库中没有：真实数据库密码、API Key、服务器密码、AccessKey、Token、Cookie、个人隐私数据等。

---

## 2. 这个项目上传前必须检查的内容

### 2.1 不要提交真实数据库密码

你当前项目的：

```text
backend/src/main/resources/application.yml
```

存在本地数据库密码配置。建议改为：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/pharmacy_delivery?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

然后本地运行时设置环境变量。

PowerShell：

```powershell
$env:DB_PASSWORD="1234"
```

注意：这里只是示例。不要把你的真实生产密码写进 README。

### 2.2 `.idea` 不需要上传

项目根目录现有 `.gitignore` 已包含：

```gitignore
.idea/
*.iml
.vscode/
backend/target/
frontend/node_modules/
frontend/dist/
*.log
.DS_Store
Thumbs.db
```

因此只要你从**项目根目录**初始化 Git，`backend/.idea` 会被忽略。

### 2.3 修正启动脚本提示

当前实际后端端口是：

```text
8089
```

Vite 代理也是：

```text
http://localhost:8089
```

但 `scripts/start-backend.bat` 里的提示文字写成了 8080。建议改为：

```bat
echo Starting Spring Boot backend on http://localhost:8089 ...
```

### 2.4 明文演示账号的处理

`database/pharmacy_delivery.sql` 中的 `admin / 123456` 等账号是演示种子数据，不是你个人真实密码，因此可以作为课程演示保留。

但 README 必须明确：当前版本是课程设计，密码为明文演示实现，不能直接用于生产环境。

---

## 3. 第一次推送：最推荐流程

以下操作建议在 **Git Bash**、IDEA Terminal 或 Windows Terminal 中执行。

假设你解压后的项目目录是：

```text
D:\Projects\pharmacy-delivery-system
```

先进入目录：

```bash
cd /d/Projects/pharmacy-delivery-system
```

### 第一步：确认你现在就在项目根目录

```bash
pwd
ls
```

你应该能看到：

```text
backend
frontend
database
scripts
.gitignore
README.md
```

### 第二步：初始化 Git

推荐：

```bash
git init -b main
```

如果你的 Git 版本较旧：

```bash
git init
git branch -M main
```

### 第三步：首次配置 Git 身份（只需配置一次）

查看：

```bash
git config --global user.name
git config --global user.email
```

如果还没配置：

```bash
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"
```

### 第四步：先检查哪些文件会进入仓库

```bash
git status
```

重点确认这些内容**不应该出现**：

```text
.idea/
node_modules/
target/
dist/
真实密码
.env
私钥
Token
```

### 第五步：加入暂存区

```bash
git add .
```

然后再次检查：

```bash
git status
```

### 第六步：创建第一次提交

```bash
git commit -m "feat: initialize pharmacy delivery system"
```

或者中文：

```bash
git commit -m "feat: 初始化药店即时配送管理系统"
```

更推荐英文提交信息，后续保持统一。

---

## 4. 在 GitHub 网站创建空仓库

在 GitHub 中选择：

```text
New repository
```

填写：

```text
Repository name: pharmacy-delivery-system
Description: 基于 Spring Boot 3 + Vue 3 + MyBatis-Plus + MySQL 的单店药店即时配送管理系统
Visibility: Public
```

**关键：第一次创建远程仓库时不要勾选：**

```text
Add a README file
Add .gitignore
Choose a license
```

因为你的本地项目已经有 README 和 `.gitignore`，远程保持空仓库最省事。

---

## 5. 连接远程 GitHub 仓库

GitHub 创建完成后会给你一个 HTTPS 地址，例如：

```text
https://github.com/<你的GitHub用户名>/pharmacy-delivery-system.git
```

在项目根目录执行：

```bash
git remote add origin https://github.com/<你的GitHub用户名>/pharmacy-delivery-system.git
```

检查：

```bash
git remote -v
```

正常应该看到：

```text
origin  https://github.com/<你的GitHub用户名>/pharmacy-delivery-system.git (fetch)
origin  https://github.com/<你的GitHub用户名>/pharmacy-delivery-system.git (push)
```

---

## 6. 推送 main 分支

执行：

```bash
git push -u origin main
```

第一次成功以后，后续通常只需要：

```bash
git push
```

---

## 7. GitHub 身份认证

GitHub 的 Git HTTPS 推送不能再使用普通账号密码作为 Git 密码。

推荐两种方式：

### 方式 A：Git Credential Manager / 浏览器登录

Windows 上通常最省事。执行 `git push` 时按照弹出的 GitHub 登录页面完成授权即可。

### 方式 B：GitHub CLI

安装 GitHub CLI 后：

```bash
gh auth login
```

按提示选择：

```text
GitHub.com
HTTPS
Login with a web browser
```

之后再执行：

```bash
git push -u origin main
```

如果手动使用 HTTPS Token，Git 提示输入 password 时输入的是 **Personal Access Token**，不是 GitHub 登录密码。

---

## 8. 以后每次修改项目怎么推送

标准工作流只有四步：

```bash
git status
git add .
git commit -m "feat: add xxx"
git push
```

例如你后来把登录密码改成 BCrypt：

```bash
git add .
git commit -m "feat: hash user passwords with BCrypt"
git push
```

修 Bug：

```bash
git commit -m "fix: restore stock when order is canceled"
```

改 README：

```bash
git commit -m "docs: improve project README"
```

---

## 9. 推荐的 Commit 类型

可以使用简化版 Conventional Commits：

| 前缀 | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat: add rider dispatch` |
| `fix` | Bug 修复 | `fix: prevent invalid order transition` |
| `docs` | 文档 | `docs: add deployment guide` |
| `refactor` | 重构 | `refactor: simplify cart service` |
| `test` | 测试 | `test: add order state machine tests` |
| `style` | 纯格式/UI | `style: optimize dashboard layout` |
| `chore` | 工程配置 | `chore: update gitignore` |

---

## 10. 常见错误处理

### `remote origin already exists`

说明已经配置过远程：

```bash
git remote -v
```

如果地址错了：

```bash
git remote set-url origin https://github.com/<你的GitHub用户名>/pharmacy-delivery-system.git
```

### `src refspec main does not match any`

通常是还没有提交。

执行：

```bash
git status
git add .
git commit -m "feat: initial commit"
git branch -M main
git push -u origin main
```

### `rejected ... non-fast-forward`

通常是你创建 GitHub 仓库时同时生成了 README，导致远程和本地各有一次初始提交。

最简单的办法是：如果远程刚创建且没有重要内容，删除远程仓库并重新创建一个**完全空仓库**。

如果远程内容需要保留，再使用：

```bash
git pull --rebase origin main
git push -u origin main
```

不要在不理解后果时直接使用 `git push --force`。

### 推送时一直要求密码

HTTPS 下不要输入 GitHub 登录密码。使用浏览器授权、Git Credential Manager、GitHub CLI，或 Personal Access Token。

---

## 11. 推送成功后 GitHub 首页建议继续完善

### About

设置：

```text
Description
Website: https://yulanlin.cn
Topics
```

### README

至少保留：

- 项目简介
- 功能
- 技术栈
- 订单状态机
- 项目结构
- 数据库
- 启动方式
- 演示账号
- 项目亮点
- 已知限制
- 后续计划

### Screenshots

建议创建：

```text
docs/images/
```

放：

```text
home.png
medicine-list.png
cart.png
order-detail.png
admin-dashboard.png
admin-orders.png
```

然后 README 中加入截图。

---

## 12. 与“星雨笔录”联动

GitHub 仓库公开后，在星雨笔录作品详情页增加：

```text
GitHub 源码 → https://github.com/<你的GitHub用户名>/pharmacy-delivery-system
```

GitHub 仓库 About 区增加：

```text
Website → https://yulanlin.cn
```

这样形成：

```text
简历
  ↓
星雨笔录作品页
  ↓
GitHub 源码
  ↓
README / 代码 / 提交记录
```

对求职项目展示来说，这比只在简历里写一段项目描述完整得多。

---

## 13. 最终发布前 Checklist

```text
[ ] README.md 已放在仓库根目录
[ ] application.yml 不包含真实数据库密码
[ ] 没有 API Key / Token / AccessKey / 私钥
[ ] .idea / node_modules / target / dist 已忽略
[ ] start-backend.bat 端口提示已改为 8089
[ ] README 明确项目为学习/课程设计用途
[ ] README 明确明文密码只是演示实现
[ ] 本地数据库脚本可以正常初始化
[ ] 后端可以启动
[ ] 前端可以启动
[ ] 用户端主流程可以跑通
[ ] 管理端订单流转可以跑通
[ ] GitHub About 已填写 Description / Website / Topics
[ ] 星雨笔录作品页已添加 GitHub 链接
[ ] 后续补充至少 4 张核心截图
```
