# 文件所有权声明 (OWNERSHIP.md)

> 本文件声明项目中每个文件/目录的所有权归属。  
> **规则**：只有文件所有者可以创建或修改该文件。其他 Agent 只能读取参考。

---

## 所有权矩阵

### 🏗️ Antigravity (架构师)

| 路径 | 说明 |
|------|------|
| `DESIGN.md` | 设计文档 |
| `CONTRACT.md` | 协作契约 |
| `OWNERSHIP.md` | 本文件 |
| `README.md` | 项目说明 |
| `.env.example` | 环境变量模板 |
| `docker-compose.yml` | 生产编排 |
| `docker-compose.dev.yml` | 开发编排 |
| `nginx/**` | Nginx 配置 |
| `docs/**` | 项目文档 (API_CONTRACT, CHANGELOG) |
| `backend/src/main/java/**/config/**` | Spring 配置类 |
| `backend/src/main/java/**/model/**` | JPA 实体类 |
| `backend/src/main/java/**/dto/**` | 数据传输对象 |
| `backend/src/main/java/**/WebApplication.java` | 主入口类 |
| `backend/src/main/resources/application*.yml` | 应用配置文件 |
| `backend/src/main/resources/db/migration/**` | Flyway 迁移脚本 |

---

### 🔧 Codex (后端)

| 路径 | 说明 |
|------|------|
| `backend/src/main/java/**/controller/**` | REST 控制器 |
| `backend/src/main/java/**/service/**` | 业务逻辑层 |
| `backend/src/main/java/**/repository/**` | 数据访问层 |
| `backend/src/main/java/**/common/**` | 公共工具、异常处理 |
| `backend/src/test/**` | 全部测试代码 |
| `backend/pom.xml` | Maven 依赖 |
| `backend/Dockerfile` | 后端容器构建 |

**只读引用**（不可修改）：
- `docs/API_CONTRACT.md` — 接口定义参考
- `backend/src/main/java/**/model/**` — 实体类引用
- `backend/src/main/java/**/dto/**` — DTO 引用
- `backend/src/main/java/**/config/**` — 配置类引用

---

### 🎨 Gemini (前端)

| 路径 | 说明 |
|------|------|
| `frontend/**` | 前端项目全部文件 |

具体包括：
- `frontend/src/api/**` — API 调用封装
- `frontend/src/assets/**` — 静态资源
- `frontend/src/components/**` — 公共组件 (含 MdEditor)
- `frontend/src/composables/**` — 组合式函数
- `frontend/src/layouts/**` — 布局组件
- `frontend/src/pages/**` — 页面视图
- `frontend/src/router/**` — 路由配置
- `frontend/src/stores/**` — Pinia 状态管理
- `frontend/src/styles/**` — 全局样式
- `frontend/src/types/**` — TypeScript 类型定义
- `frontend/src/App.vue` — 根组件
- `frontend/src/main.ts` — 入口文件
- `frontend/package.json` — 前端依赖
- `frontend/vite.config.ts` — Vite 配置
- `frontend/Dockerfile` — 前端容器构建

**只读引用**（不可修改）：
- `docs/API_CONTRACT.md` — 接口定义参考
- `backend/src/main/java/**/dto/**` — DTO 结构参考（前端 TS 类型需对齐）

---

## 所有权争议解决

1. 如果不确定某文件的所有权，默认归 **Antigravity** 所有
2. 新增文件/目录必须落在自己拥有的路径下
3. 如需在他人区域新增文件，须在任务报告中申请
4. 所有权变更须由 **Antigravity** 更新本文件
