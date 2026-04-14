---
name: smartcrew-project-baseline
description: SmartCrew 项目基础信息速览，用于新对话开场快速建立上下文
---

# SmartCrew Project Baseline Skill

## 1. 项目定位
SmartCrew-Agent 是一个多模块智能体平台工程，核心目标是支持：
- 多 Agent 管理与分发
- Prompt 模板与偏好管理
- 平台接入（飞书/企微）与自有 Web 交互
- 管理后台可视化运营

## 2. 模块结构（必须知道）
- `smartcrew-admin`
  - 启动模块 + 控制器 + 集成测试
  - 当前主要页面服务接口位于 `/api/admin/*` 与 `/api/web/*`
- `smartcrew-modules`
  - 核心业务实现（Agent、Prompt、会话、用户等）
- `smartcrew-modules-api`
  - 领域实体、VO、Mapper、Service 接口契约
- `smartcrew-common`
  - 公共能力（返回结构、异常、分页、配置）
- `smartcrew-ui`
  - Vue 3 前端工程（公众页 + 后台页）
- `docs`
  - 教程、手册、技术洞察与风格 skill

## 3. 关键运行信息
- 后端默认端口：`8085`
- 前端目录：`smartcrew-ui`
- 常见后端启动配置：`smartcrew-admin/src/main/resources/application*.yml`

## 4. 接口分层约定
- `/api/v1/*`
  - 既有主链路，强调兼容，不轻易破坏语义
- `/api/web/*`
  - 公众页面服务接口
- `/api/admin/*`
  - 后台管理服务接口

默认原则：
- 新能力优先走 `web/admin`，避免影响 `v1` 既有接入方
- 是否启用由配置控制，支持裁剪部署

## 5. 分页统一约定
- 后端统一复用：
  - `PageQuery`
  - `TableDataInfo`
  - `MyBatis-Plus PaginationInnerInterceptor`
- 兼容策略：
  - 显式传 `pageNum/pageSize` -> 分页查询
  - 不传分页参数 -> 保持原全量语义（避免误伤历史页面）

## 6. 前端风格与交互基线
- 主题：浅色液态玻璃（Liquid Glass）
- 文案：中文
- 布局：固定视口 + 区块内滚动
- 分页：`10/30/50/100/250` + `total, sizes, prev, pager, next, jumper`

详细视觉规范请读取：
- `docs/skills/SMARTCREW_UI_STYLE_SKILL.md`

## 7. 新任务开场建议流程
1. 先读本文件，建立项目全局上下文。
2. 若涉及前端页面，读 `SMARTCREW_UI_STYLE_SKILL.md`。
3. 若涉及业务链路，按 `controller -> service -> mapper -> ui/api` 顺序定位。
4. 变更后至少做一次编译或构建验证。
