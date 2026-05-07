# Contributing

感谢你关注 SmartCrew-Agent，欢迎通过 Issue 和 Pull Request 参与改进。

## 开始之前

在提交代码前，建议先阅读以下文档：

- [README.md](README.md)
- [docs/DEVELOPER_MANUAL.md](docs/DEVELOPER_MANUAL.md)
- [docs/skills/PROJECT_BASELINE_SKILL.md](docs/skills/PROJECT_BASELINE_SKILL.md)
- [docs/skills/SMARTCREW_UI_STYLE_SKILL.md](docs/skills/SMARTCREW_UI_STYLE_SKILL.md)

## 贡献范围

欢迎以下类型的贡献：

- Bug 修复
- 文档补充与示例完善
- Agent / Tool / RAG 能力扩展
- 管理后台与前端体验优化
- 测试、构建、工程化和安全基线改进

## 开发约定

请尽量遵守以下项目约定：

- 优先在现有模块内做最小改动，避免无必要的大范围重构
- 尽量保持 `/api/v1` 兼容，新能力优先放在 `/api/web` 或 `/api/admin`
- 分页能力优先复用 `PageQuery + TableDataInfo + MyBatis-Plus`
- 前端页面保持现有统一视觉风格，不额外引入新的主题系统
- 代码注释、页面文案和文档默认使用中文

## 安全要求

请不要提交以下内容：

- 真实 API Key、数据库密码、访问令牌、私钥、证书文件
- 本地专用 `.env`、`application-local.yml`、生产环境配置
- 仅适用于你个人机器的临时调试数据或构建产物

如果你的改动涉及鉴权、密钥、权限控制或外部平台接入，请在 PR 描述中明确说明影响范围和验证方式。

## 本地开发

### 后端

```bash
mvn -pl smartcrew-admin -am package
```

```bash
mvn -pl smartcrew-admin spring-boot:run
```

### 前端

```bash
cd smartcrew-ui
npm install
npm run dev
```

## 提交前检查

提交前建议至少完成以下检查：

1. 运行与你改动相关的构建、测试或手工验证
2. 确认没有引入敏感信息、无关文件和临时调试代码
3. 确认新增接口、配置项、数据库结构变更已补充说明
4. 如果改动影响用户行为或页面展示，补充截图或验证结果

## Pull Request 流程

1. 先通过 Issue 说明问题、需求或设计方向
2. Fork 仓库并创建独立分支
3. 提交聚焦且可审阅的改动
4. 在 PR 描述中说明变更目的、影响范围、验证结果和风险点
5. 等待 Review，并根据反馈继续完善

## Commit 建议

推荐使用清晰、聚焦的提交信息，例如：

- `feat: 新增知识库文档分片策略`
- `fix: 修复后台登录状态失效问题`
- `docs: 更新 README 与部署说明`
- `chore: 清理开源前仓库元信息`

## 问题反馈

- 普通缺陷、需求建议：欢迎直接提交 Issue
- 潜在安全问题：请先不要公开披露利用细节，待项目维护者补充正式安全联系渠道后，再按约定私下反馈
