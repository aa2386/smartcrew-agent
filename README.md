# SmartCrew-Agent

## 导航

- [开发手册](docs/DEVELOPER_MANUAL.md)
- [项目基础 Skill](docs/skills/PROJECT_BASELINE_SKILL.md)
- [前端风格 Skill](docs/skills/SMARTCREW_UI_STYLE_SKILL.md)

## 项目简介

SmartCrew-Agent 是一个基于 Spring Boot 3、LangChain4j、MyBatis-Plus 的多模块 Java 项目脚手架，面向“多代理协作 + 工具注册执行 + 用户记忆 + 提示词模板 + 平台事件接入”这类智能体应用场景。

当前仓库更偏向基础骨架和模块拆分示例，已经具备代理注册、工具元数据管理、简单决策规划、用户偏好记忆、提示词模板管理以及平台适配器占位接入等能力，适合作为后续扩展智能体平台的起点工程。

## 技术栈与运行环境

- Java 17
- Maven 3.9+
- Spring Boot 3.4.4
- MyBatis-Plus 3.5.11
- MySQL 8.x
- LangChain4j 1.2.0
- Hutool 5.8.35
- OkHttp 4.12.0
- PlantUML 1.2024.3

## 模块结构说明

- `smartcrew-common`
  - 公共能力模块
  - 提供统一返回体、异常处理、分页模型、配置类和工具类
- `smartcrew-modules-api`
  - API 契约模块
  - 提供实体、请求对象、视图对象、Mapper 接口和 Service 接口
- `smartcrew-modules`
  - 核心实现模块
  - 提供代理、工具、记忆、提示词、MCP、平台适配器等实现
- `smartcrew-admin`
  - 管理端与启动模块
  - 提供 Spring Boot 启动入口、REST 控制器、资源配置和测试代码
- `sql`
  - 初始化脚本目录
  - 包含项目启动所需的数据库建表脚本

## 核心能力概览

- 代理管理
  - 支持代理定义注册、查询和按编码派发
  - 内置 `echo-agent`、`planner-agent` 示例代理
  - 支持将数据库中定义但尚未实现的代理注册为占位代理
- 工具注册与执行
  - 支持工具元数据刷新、工具定义维护、启用/禁用管理
  - 当前内置基础工具、文件工具、终端工具、网页搜索、网页读取、图片搜索、文档读取、PlantUML 等能力
- 用户记忆与偏好
  - 支持按用户查询偏好
  - 支持新增或更新用户偏好
  - 会话记忆服务基于用户偏好服务进行封装
- 提示词模板
  - 支持提示词模板新增、全量查询、按分类获取最新模板
- 决策规划
  - 提供占位版 ReAct 决策计划生成能力
  - 返回思考过程、步骤、建议工具和下一步动作
- 平台事件接入
  - 提供 `wecom`、`feishu` 两个平台适配器占位实现
  - 当前主要用于演示平台事件分发流程

## 环境准备

在启动项目之前，请先确认本地已经准备好以下环境：

1. 安装 Java 17，并正确配置 `JAVA_HOME`
2. 安装 Maven，并确保命令行可执行 `mvn -v`
3. 启动 MySQL 数据库服务
4. 创建数据库 `smartcrew_agent`

示例建库语句：

```sql
CREATE DATABASE smartcrew_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

## 数据库初始化

项目初始化脚本位于：

- `sql/init-smartcrew-agent.sql`

执行方式示例：

```bash
mysql -uroot -proot smartcrew_agent < sql/init-smartcrew-agent.sql
```

脚本会初始化以下核心表：

- `prompt_template`
- `mcp_info`
- `user_preference`
- `agent_definition`
- `tool_definition`
- `agent_tool_binding`

## 配置文件说明

### 1. 主配置文件

文件位置：

- `smartcrew-admin/src/main/resources/application.yml`

主要用途：

- 指定应用名称和激活环境
- 配置服务端口
- 配置 MyBatis-Plus 基础参数
- 配置 SmartCrew 的默认工具开关与基础能力

当前默认端口：

- `8085`

### 2. 开发环境配置文件

文件位置：

- `smartcrew-admin/src/main/resources/application-dev.yml`

主要用途：

- 配置 MySQL 数据源
- 配置 Tavily 搜索接口参数
- 配置 Pexels 图片搜索接口参数

你至少需要根据本地环境修改以下内容：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

### 3. 关键配置项说明

#### LLM 配置

`application.yml` 中默认包含以下配置：

```yaml
smartcrew:
  llm:
    enabled: false
    provider: openai
    model: gpt-4o-mini
```

说明：

- `enabled=false` 表示默认不启用大模型调用
- 当前项目保留了 LLM 配置入口，但并未在所有业务链路中完全打通

#### 工具开关配置

默认工具开关位于：

```yaml
smartcrew:
  tools:
    enabled:
      basic: true
      file: true
      image-search: true
      plantuml: true
      web-search: true
      web-page: true
      document: true
      terminal: false
```

说明：

- `terminal` 默认关闭，避免在未明确授权的情况下执行命令
- 其他工具默认开启，但部分工具依赖外部 API Key

#### 工具外部依赖配置

`application-dev.yml` 中可配置以下参数：

```yaml
smartcrew:
  tooling:
    tavily:
      api-key:
      base-url: https://api.tavily.com/search
    pexels:
      api-key:
      api-url: https://api.pexels.com/v1/search
```

说明：

- 未配置 Tavily API Key 时，网页搜索工具无法正常调用
- 未配置 Pexels API Key 时，图片搜索工具无法正常调用

#### 文件工具目录

默认文件工具目录位于：

```yaml
smartcrew:
  tooling:
    file:
      save-dir: ./tmp
```

说明：

- 文件工具和终端工具会基于该目录进行读写或执行命令

## 编译与启动步骤

### 1. 清理并编译项目

在仓库根目录执行：

```bash
mvn clean package
```

### 2. 启动管理端应用

可直接运行启动类：

- `smartcrew-admin/src/main/java/com/smartcrew/agent/SmartCrewAgentApplication.java`

或者在模块目录执行：

```bash
mvn -pl smartcrew-admin spring-boot:run
```

### 3. 访问服务

启动成功后，可通过以下地址访问项目接口：

```text
http://localhost:8085
```

## 默认配置与注意事项

- 当前 `wecom` 与 `feishu` 平台适配器为占位实现，主要用于演示事件分发流程
- 当前决策引擎 `ReActDecisionEngine` 返回的是占位型规划结果，适合作为后续扩展入口
- `DefaultToolExecutor` 当前主要完成工具元数据校验和 Bean 定位，尚未完全实现动态方法调用
- 终端工具 `terminal` 默认关闭，这是一个偏高风险能力，建议仅在受控环境下开启
- 图片搜索和网页搜索依赖外部 API，如果未配置密钥会抛出业务异常
- 项目中已经包含基础测试与 H2 测试配置，可在后续扩展业务时继续完善

## 建议的开发接入顺序

如果你准备在这个仓库上继续开发，建议按下面的顺序阅读与接手：

1. 先阅读 `smartcrew-admin` 下的控制器，了解目前暴露了哪些能力入口
2. 再阅读 `smartcrew-modules-api`，理解领域对象、请求响应模型和服务契约
3. 最后阅读 `smartcrew-modules`，重点关注代理注册、工具注册、记忆服务和决策引擎实现

这样能更快从“接口入口”过渡到“核心实现”和“后续扩展点”。
