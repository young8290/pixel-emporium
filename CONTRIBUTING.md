# 贡献指南

感谢你愿意为 Pixel Emporium（像素百货）出一份力！

## 开发流程

1. Fork 仓库并从 `main` 创建分支，例如 `feat/cart-coupon`、`fix/order-cancel`。
2. 按 [README 快速开始](README.md#-快速开始) 准备本地环境。
3. 完成修改后，在提交前运行测试：

   ```bash
   ./mvnw test
   npm test --prefix frontend
   npm test --prefix frontend-admin
   ```

4. 前端代码可运行 `npm run lint --prefix <frontend|frontend-admin>` 检查风格。
5. 提交 Pull Request，按模板说明改动内容与验证方式。

## 提交信息

推荐使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 风格：

```text
feat: 购物车支持优惠券
fix: 修复取消订单后库存未回滚
docs: 补充 SMTP 配置说明
```

## 代码约定

- 遵循仓库根目录的 `.editorconfig`（Java 4 空格缩进，前端 2 空格）。
- 数据库结构变更只新增 Flyway 迁移脚本（`*/src/main/resources/db/migration/V<n>__<desc>.sql`），不要修改已发布的迁移。
- 新增功能请附带对应的单元测试或接口测试。
- 新增像素素材请同时提供 `@1x` 与 `@2x` 两种尺寸，并放到 `assets/pixel/` 下对应分类目录。

## 安全与凭据

- 不要提交真实密码、私钥、Token 或第三方密钥；使用环境变量和本地 `.env.local`。
- 发现安全问题请参阅 [SECURITY.md](SECURITY.md)，不要公开提交 Issue。

## 许可证

提交贡献即表示你同意你的代码以 [MIT License](LICENSE) 授权。
