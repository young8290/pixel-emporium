# 安全策略

Pixel Emporium 是一个本地学习与实训项目，不提供生产环境支持。即便如此，我们仍欢迎负责任地报告安全问题。

## 报告漏洞

请**不要**通过公开 Issue 报告安全漏洞。请使用 GitHub 仓库的
[Security → Report a vulnerability](../../security/advisories/new)（私密安全通告）提交，并尽量包含：

- 受影响的模块（`user`、`business`、`gateway`、`frontend`、`frontend-admin` 等）
- 复现步骤或概念验证
- 可能造成的影响

## 凭据泄露

如果你不慎提交了密码、私钥、Token 或第三方密钥：

1. 立即在对应平台（支付宝沙箱、SMTP 服务商、OSS 等）撤销或轮换该凭据；
2. 再从 Git 历史中清理相关内容。

仅删除文件并不能让已泄露的凭据失效。
