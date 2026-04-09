# SmartCrew UI

Vue 3 单工程前端，包含两套独立界面：

- 公众聊天页：`/`
- 后台管理页：`/admin/*`

## 环境变量

参考 `.env.example`：

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_ENABLE_WEB=true
VITE_ENABLE_ADMIN=true
```

## 启动命令

```bash
npm install
npm run dev
```

## 说明

- 所有页面文案均采用中文。
- `VITE_ENABLE_WEB` 与 `VITE_ENABLE_ADMIN` 可配合后端开关裁剪功能入口。
- 当前请求层默认对接 `/api/web/*` 与 `/api/admin/*`。
