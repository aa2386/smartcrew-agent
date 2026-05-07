# SmartCrew-Agent

SmartCrew-Agent 是一个基于 Spring Boot 3、LangChain4j、MyBatis-Plus 和 Vue 3 的多模块智能体平台示例工程，面向多 Agent 协作、Prompt 编排、工具注册执行、用户记忆、RAG 检索增强以及 Web / Admin 管理场景。

项目当前更适合作为智能体平台的基础脚手架与学习型工程，而不是“开箱即用”的完整生产方案。你可以在这个仓库上继续扩展 Agent 运行时、工具体系、知识库能力、平台接入和后台管理界面。

## 开源信息

- 开源许可证：[MIT License](LICENSE)
- 贡献指南：[CONTRIBUTING.md](CONTRIBUTING.md)
- 变更记录：[CHANGELOG.md](CHANGELOG.md)

## 项目特性

- 多模块后端架构，便于拆分领域模型、业务实现与启动模块
- 同时提供 `/api/v1`、`/api/web`、`/api/admin` 三层接口体系
- 支持本地 JWT 鉴权、用户注册登录、管理员后台能力
- 支持 Agent 定义、Prompt 模板、工具绑定、用户偏好管理
- 支持 RAG 基础设施、知识库、文档切片与向量检索
- 提供 Vue 3 前端工程，包含公众端与后台管理端
- 保留平台接入扩展点，便于继续接入企业微信、飞书等外部平台

## 项目结构

- `smartcrew-admin`
  Spring Boot 启动模块、控制器层、资源配置、集成测试
- `smartcrew-common`
  公共配置、异常处理、分页模型、工具类
- `smartcrew-modules-api`
  领域实体、VO、Mapper、Service 接口契约
- `smartcrew-modules`
  Agent、Prompt、鉴权、RAG、工具、平台适配等核心业务实现
- `smartcrew-ui`
  Vue 3 前端工程，包含公众聊天页与后台页面
- `sql`
  数据库初始化脚本
- `docs`
  开发手册、技术文档、测试文档和仓库内技能说明

## 技术栈

- Java 17
- Maven 3.9+
- Spring Boot 3.4.4
- MyBatis-Plus 3.5.11
- MySQL 8.x
- H2（测试环境）
- LangChain4j 1.0.0-beta2
- Chroma Vector Store
- Vue 3
- Vite 6
- Element Plus

## 快速开始

### 1. 环境准备

- 安装 Java 17 并正确配置 `JAVA_HOME`
- 安装 Maven，并确保 `mvn -v` 可用
- 安装 MySQL 8.x
- 可选：准备 Chroma 服务，用于 RAG 向量存储
- 可选：安装 Node.js 18+，用于启动前端

### 2. 初始化数据库

先创建数据库：

```sql
CREATE DATABASE smartcrew_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后执行初始化脚本：

```bash
mysql -u root -p smartcrew_agent < sql/init-smartcrew-agent.sql
```

### 3. 配置本地环境变量

后端推荐配置以下变量；如果用于共享环境或正式部署，建议全部显式设置，不要依赖默认值：

```bash
MYSQL_DB_USERNAME=root
MYSQL_DB_PASSWORD=your-db-password
SMARTCREW_TOKEN_SECRET=replace-with-a-strong-secret
SMARTCREW_BOOTSTRAP_ADMIN_ENABLED=true
SMARTCREW_BOOTSTRAP_ADMIN_PASSWORD=replace-with-a-strong-admin-password
```

如果你要启用模型或外部工具，还需要按需配置：

```bash
DASHSCOPE_API_KEY=your-dashscope-api-key
TAVILY_API_KEY=your-tavily-api-key
PEXELS_API_KEY=your-pexels-api-key
```

### 4. 启动后端

在仓库根目录执行：

```bash
mvn -pl smartcrew-admin spring-boot:run
```

默认端口：

- 后端服务：`http://localhost:8085`

### 5. 启动前端

在 `smartcrew-ui` 目录执行：

```bash
npm install
npm run dev
```

常用前端脚本：

- `npm run dev`
- `npm run build`
- `npm run preview`

## 构建与测试

全量构建：

```bash
mvn clean package
```

仅启动模块构建：

```bash
mvn -pl smartcrew-admin -am package
```

执行测试：

```bash
mvn -pl smartcrew-admin -am test -DskipTests=false
```

说明：

- 测试环境默认使用 H2 内存数据库
- 如果你扩展了 RAG、工具或平台接入能力，建议同时补充对应集成测试

## 关键配置说明

主要配置文件位于：

- `smartcrew-admin/src/main/resources/application.yml`
- `smartcrew-admin/src/main/resources/application-dev.yml`
- `smartcrew-admin/src/test/resources/application-test.yml`

当前接口分层约定：

- `/api/v1/*`
  兼容型接口，新增能力应尽量避免破坏既有语义
- `/api/web/*`
  面向公众端页面的服务接口
- `/api/admin/*`
  面向后台管理页面的服务接口

## 安全说明

- 仓库默认不应提交真实 API Key、密码、私钥、证书文件或本地环境配置
- 在共享环境或正式部署中，务必显式配置 `SMARTCREW_TOKEN_SECRET`
- 启用默认管理员初始化时，务必显式配置 `SMARTCREW_BOOTSTRAP_ADMIN_PASSWORD`
- 请不要在 `application-local.yml`、`.env`、证书文件中写入真实密钥后再提交
- 正式部署前，请替换所有本地测试用配置，并限制跨域来源、数据库权限与管理员初始化策略

## 文档索引

- 开发手册：`docs/DEVELOPER_MANUAL.md`
- 项目基线说明：`docs/skills/PROJECT_BASELINE_SKILL.md`
- 前端风格规范：`docs/skills/SMARTCREW_UI_STYLE_SKILL.md`
- 技术文档：`docs/technical-docs`

## 适用场景

- 想快速搭建一个可扩展的多 Agent 平台原型
- 想学习 LangChain4j 在 Java 工程中的落地方式
- 想基于现有后台和前端页面继续扩展 Prompt、Tool、RAG、会话管理能力

不建议直接用于未审计的生产环境，尤其是在鉴权、审计、限流、隔离和密钥管理尚未补强之前。

## 开发建议

- 新增后端接口优先放在 `/api/web` 或 `/api/admin`
- 尽量保持 `/api/v1` 兼容
- 分页能力建议复用 `PageQuery + TableDataInfo + MyBatis-Plus`
- 前端页面建议保持现有统一视觉风格，不额外引入新主题系统

## 贡献方式

欢迎通过 Issue 和 Pull Request 参与改进，具体流程和约定请参考 [CONTRIBUTING.md](CONTRIBUTING.md)。

如果你的改动涉及接口、配置、安全策略或数据库结构，请在 PR 描述中明确说明影响范围、验证结果和风险点。

## 路线建议

这个仓库后续可以优先补强以下方向：

- 更清晰的 Agent 编排与运行时抽象
- 更完整的工具权限与风险控制
- 更稳定的 RAG 文档处理与向量检索链路
- 更规范的部署、监控和审计能力
- 更完善的安全响应与发布流程，例如 `SECURITY`、版本发布说明、升级指南

## License

本项目当前采用 [MIT License](LICENSE) 开源发布。
