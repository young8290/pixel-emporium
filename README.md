# Pixel Emporium

Pixel Emporium 是一个像素风全栈商城实训项目。界面保留“像素百货”的中文品牌与像素化视觉主题，后端采用 Spring Boot 微服务，前端采用 Vue 3，适合本地学习、演示和课程实训，不面向真实生产交易。

## 功能概览

- 邮箱验证码注册、登录、JWT 鉴权、地址簿和个人资料管理。
- 商品、分类树、SKU/规格、折扣价、搜索建议、条件筛选、销量/评分排序和相关推荐。
- 商品评分与评论：用户可提交、修改和删除自己的评论，管理员可审核、隐藏或恢复评论。
- 收藏夹、浏览历史、购物车、库存、订单创建/取消/确认和订单详情。
- 支付宝沙箱支付接口，以及仅用于实训演示的可选模拟支付路径。
- 管理端仪表盘、商品/SKU、分类、订单、评论、用户、操作日志和 SMTP 邮件中心。
- 邮件验证码、下单订单详情、HTML 邮件模板预览、投递记录与失败重试。

## 架构与端口

| Component | Directory | Default port | Responsibility |
| --- | --- | ---: | --- |
| Storefront | `frontend` | 5173 | Customer shopping and account UI |
| Admin UI | `frontend-admin` | 6173 | Back-office management UI |
| Gateway | `gateway` | 8000 | Unified API entry and JWT filtering |
| User service | `user` | 8080 | Accounts, addresses, RBAC, SMTP, and mail delivery |
| Business service | `business` | 8081 | Catalog, cart, orders, payment, reviews, and recommendations |

`common`、`common-orm` 和 `common-security` 提供共享模型、数据库和鉴权能力；`generator` 是可选的代码生成模块。用户端和管理端都通过 Gateway 访问后端。项目要求 JDK 21、Node.js `^20.19.0 || >=22.12.0`、npm、MySQL 8 和 Redis 7。用户服务与商城服务启动时会由 Flyway 自动执行各自的数据库迁移。

## 本地配置

仓库保留以下三份可跟踪的开发配置，所有敏感值都为空环境变量占位符：

- `user/src/main/resources/application-dev.yml`
- `business/src/main/resources/application-dev.yml`
- `gateway/src/main/resources/application-dev.yml`

启动前请在当前终端设置所需环境变量。`JWT_SECRET` 必须在三个后端服务之间保持一致；RSA 私钥只放在本机环境中，前端通过各自的 `.env.local` 提供 RSA 公钥（可从 `.env.local.example` 复制）。不要提交真实密码、私钥、Token 或第三方密钥。

核心配置变量：

```text
USER_DB_PASSWORD, BUSINESS_DB_PASSWORD, JWT_SECRET, RSA_PRIVATE_KEY
MAIL_CONFIG_MASTER_KEY_BASE64, MAIL_INTERNAL_HMAC_SECRET, MAIL_VERIFICATION_PEPPER
```

可选能力变量：

```text
ALIPAY_ENABLED, ALIPAY_APP_ID, ALIPAY_SELLER_ID, ALIPAY_PRIVATE_KEY
ALIPAY_PUBLIC_KEY, ALIPAY_NOTIFY_URL, ALIPAY_RETURN_URL
OSS_ENABLED, OSS_ENDPOINT, OSS_ACCESS_KEY, OSS_SECRET_KEY, OSS_BUCKET
PIXEL_STYLE_ENABLED, PIXEL_STYLE_API_BASE_URL, PIXEL_STYLE_API_KEY, PIXEL_STYLE_API_MODEL
```

SMTP 不把密码写入配置文件，启动后使用管理员端“邮件设置”页面配置并加密保存。未配置 SMTP 时，商城仍可浏览已有商品，但邮箱验证码和订单邮件投递会被服务拒绝；未启用 OSS 时无法使用远程对象存储上传；未启用图像模型时无法执行商品像素风格化；未启用支付宝时可在 `PAYMENT_MOCK_ENABLED=true` 下使用训练用模拟支付。

## 初始化数据库

先启动 MySQL 和 Redis，创建两个数据库：

```sql
CREATE DATABASE `sb-shop-user` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE `sb-shop-business` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 启动开发环境

在项目根目录打开不同终端：

```powershell
.\mvnw.cmd -pl user spring-boot:run
.\mvnw.cmd -pl business spring-boot:run
.\mvnw.cmd -pl gateway spring-boot:run

npm ci --prefix frontend
npm run dev --prefix frontend
npm ci --prefix frontend-admin
npm run dev --prefix frontend-admin
```

访问地址：用户端 `http://localhost:5173`，管理端 `http://localhost:6173`，Gateway API `http://localhost:8000`。管理员账号需要先按项目初始化数据或自行注册并授予相应角色。

## 测试与生产构建

```powershell
.\mvnw.cmd test
npm test --prefix frontend
npm test --prefix frontend-admin

.\mvnw.cmd package
npm run build --prefix frontend
npm run build --prefix frontend-admin
```

构建结果位于各前端的 `dist/` 和各 Maven 模块的 `target/`，这些目录均由 `.gitignore` 排除，可随时重新生成。依赖安装必须使用提交的 `package-lock.json`；推荐使用 `npm ci` 以获得可复现安装。

## 目录结构

```text
pixel-emporium/
├─ frontend/             # Vue 用户端
├─ frontend-admin/       # Vue 管理端（含 Vditor）
├─ gateway/               # Spring Cloud Gateway
├─ user/                  # 用户、权限、邮件服务
├─ business/              # 商品、订单、支付、评论服务
├─ common/                # 公共响应与基础组件
├─ common-orm/            # MyBatis-Plus 与数据库公共配置
├─ common-security/       # JWT 与请求鉴权
├─ generator/             # 可选代码生成器
├─ .mvn/                  # Maven Wrapper 配置
├─ mvnw, mvnw.cmd         # Maven Wrapper 启动脚本
└─ pom.xml                # Maven 聚合根项目
```

## 实训边界

本仓库只保证本地实训场景下的可构建性，不提供线上部署、真实支付商户配置、生产级监控、SLA 或托管演示地址。支付宝、SMTP、OSS 和图像模型均为可选外部能力；请使用沙箱账号和临时凭据，并在实验结束后撤销相关密钥。
