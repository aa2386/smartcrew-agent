# SmartCrew Agent Instructions

## 首次进入仓库必须先读取
1. `docs/skills/PROJECT_BASELINE_SKILL.md`
2. `docs/skills/SMARTCREW_UI_STYLE_SKILL.md`（涉及前端页面时必读）

## 任务执行约定
- 优先在现有模块内做最小改动，不破坏 `api/v1` 兼容性。
- 新增后端接口优先放在 `api/web` 或 `api/admin`，并保留配置化开关思路。
- 涉及分页时复用 `PageQuery + TableDataInfo + MyBatis-Plus` 体系。
- 涉及前端新增页面时，必须复用全局液态玻璃风格，不另起主题系统。

## 输出约定
- 代码注释与页面展示文案默认中文。
- 说明变更时，优先给出“影响范围 + 验证结果 + 风险点”。
