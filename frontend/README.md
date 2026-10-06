# 个性化学习推荐系统 · 前端

个性化学习推荐系统的前端工程，基于 Vue 3 + Vite + Element Plus，开发环境通过代理与后端（Spring Boot）通信。

## 技术栈

- Vue 3（`<script setup>`）
- Vite 7
- Element Plus + @element-plus/icons-vue
- Vue Router（路由守卫鉴权）
- Vuex
- Axios
- WindiCSS
- NProgress

## 环境要求

- Node.js 18+（推荐 24）
- npm 11+

## 启动

```bash
npm install
npm run dev
```

默认地址：http://localhost:5173

`/api` 请求在开发环境自动代理到 http://localhost:8080（见 `vite.config.js`），需先启动后端。

## 构建

```bash
npm run build      # 产物输出到 dist/
npm run preview    # 本地预览构建产物
```

## 目录结构

```text
src/
├─ api/             # 接口封装
├─ components/      # 通用组件
├─ composables/     # 组合式函数（鉴权、列表、Tab）
├─ layouts/         # 布局（头部、菜单、标签页）
├─ pages/           # 页面
│  ├─ course/       # 课程列表、详情
│  ├─ study/        # 推荐、我的学习
│  ├─ teach/        # 发布课程、我的课程
│  └─ operation/    # 用户 / 课程管理
├─ router/          # 路由配置
├─ store/           # Vuex
├─ axios.js         # Axios 实例与拦截器
├─ permission.js    # 全局路由守卫
└─ main.js          # 入口
```
