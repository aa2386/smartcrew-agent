---
name: smartcrew-ui-style
description: SmartCrew 前端页面风格规范（液态玻璃 + 浅色高级感），用于新增页面快速对齐现有视觉体系
---

# SMARTCREW UI Style Skill

## 1. 目标
本规范用于新增 `smartcrew-ui` 页面时，快速复用当前已上线的视觉语言，确保新页面和现有页面在以下方面保持一致：
- 风格：浅色液态玻璃（Liquid Glass）
- 语气：简约、现代、偏管理系统高级感
- 可读性：中文内容清晰、对比度充足
- 结构：固定视口 + 区块内滚动

## 2. 必须遵守
- 页面显示文本使用中文。
- 不新增与现有系统冲突的主题色、阴影体系、圆角体系。
- 复用全局样式变量与通用类，优先使用 `src/styles/base.scss`。
- 复杂列表必须使用“容器内滚动”，不能把整页高度撑长。
- 分页统一使用同一套参数与交互（见第 5 节）。

## 3. 视觉 Token（直接复用）
来源文件：[base.scss](C:/WorkFile/Learning_Code/smartcrew-agent/smartcrew-ui/src/styles/base.scss)

核心变量（不可随意改名）：
- 字体：`--sc-font-title`、`--sc-font-body`
- 主色：`--sc-primary`、`--sc-primary-deep`
- 辅助色：`--sc-accent`、`--sc-support`
- 文本：`--sc-text`、`--sc-text-soft`
- 玻璃边框与面板：`--sc-border`、`--sc-panel`、`--sc-panel-strong`
- 阴影：`--sc-shadow`、`--sc-shadow-soft`
- 圆角：`--sc-radius-lg`、`--sc-radius-md`、`--sc-radius-sm`

固定视觉基调：
- 背景为浅色多层渐变 + 光斑，不使用纯平单色底。
- 卡片需有半透明、模糊、边缘高光、柔和阴影。
- 控件（按钮/输入/弹框/表格/分页）统一玻璃皮肤。

## 4. 页面骨架规范
- 根容器优先使用：`.page-shell` / `.page-grid` / `.admin-card` / `.page-card`。
- 列表区域统一放在 `.table-shell`，并给表格 `height="100%"`。
- 可滚动内容区域统一用 `.scroll-pane` 或等价样式：
  - `min-height: 0;`
  - `overflow: auto;`
  - 必要时 `padding-right: 6px;`
- 弹框、抽屉、下拉默认走 Element Plus，全局主题已注入玻璃风格，不另起一套样式。

## 5. 分页统一规范
前端分页参数与行为固定如下：
- 分页状态：`pageNum`、`pageSize`、`total`
- 默认：`pageNum = 1`，`pageSize = 10`
- 每页可选：`[10, 30, 50, 100, 250]`
- 组件布局：`total, sizes, prev, pager, next, jumper`
- 条件变化后必须重置到第一页：`pageNum = 1`

标准模板：

```vue
<el-pagination
  :current-page="pager.pageNum"
  :page-size="pager.pageSize"
  :page-sizes="[10, 30, 50, 100, 250]"
  :total="pager.total"
  layout="total, sizes, prev, pager, next, jumper"
  @current-change="handlePageChange"
  @size-change="handleSizeChange"
/>
```

```ts
const pager = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function handlePageChange(pageNum: number) {
  pager.pageNum = pageNum
  await loadList()
}

async function handleSizeChange(pageSize: number) {
  pager.pageSize = pageSize
  pager.pageNum = 1
  await loadList()
}
```

## 6. 组件与交互建议
- 卡片容器优先使用 [GlassPanel.vue](C:/WorkFile/Learning_Code/smartcrew-agent/smartcrew-ui/src/components/common/GlassPanel.vue)。
- 管理页推荐布局：左筛选/列表 + 右详情（或单栏列表 + 抽屉详情）。
- 表格操作按钮文案简洁统一：`查看`、`编辑`、`删除`、`保存`、`查询`。
- 状态文案统一中文：`启用/停用`、`暂无`、`未选择`。

## 7. 新增页面执行清单
新增页面时按以下顺序执行：
1. 先套用现有页面骨架类（不要先写自定义布局）。
2. 接入 API 后，再补分页与筛选联动。
3. 检查桌面与移动端（<1200px、<700px）下是否可用。
4. 检查是否出现“整页被列表撑高”的问题。
5. 检查是否有英文遗留文案，统一改为中文。

## 8. 不建议做的事
- 不单独引入新的 UI 主题库覆盖 Element Plus。
- 不在单页面内定义与全局冲突的“新玻璃变量”。
- 不使用深色背景回退到旧风格（除非明确需求变更）。
- 不为了视觉效果牺牲可读性（浅色背景上禁止低对比文字）。
