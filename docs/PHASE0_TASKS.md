# Phase 0 任务下发 — 提示词

> 以下是分别给 Codex 和 Gemini 的提示词。**可以同时下发，两者互不依赖**。  
> 下发前确保 Git 仓库已初始化且在 `develop` 分支上。

---

## 🔧 给 Codex 的提示词（后端）

> 在终端中打开 Codex，将以下内容粘贴给它：

```
你是这个项目的后端开发者。请仔细阅读项目根目录下的 CONTRACT.md、OWNERSHIP.md 和 docs/API_CONTRACT.md，然后完成 Phase 0 后端脚手架搭建。

### 任务：[Phase 0] 后端脚手架搭建

### 上下文
- 本项目是一个 Spring Boot + Vue 3 的个人科研工具网站
- 你只负责后端代码，严格遵守 OWNERSHIP.md 中的文件权限
- 接口定义参考 docs/API_CONTRACT.md

### 具体要求

1. **创建 Spring Boot 项目**（在 `backend/` 目录下）：
   - Java 17, Spring Boot 3.x, Maven
   - groupId: `com.changye`, artifactId: `personal-web`
   - pom.xml 引入以下依赖：
     - spring-boot-starter-web
     - spring-boot-starter-data-jpa
     - spring-boot-starter-validation
     - postgresql (runtime)
     - h2 (test/dev)
     - lombok
     - flyway-core
     - spring-boot-starter-test

2. **创建主入口类** `WebApplication.java`（包路径 `com.changye.web`）

3. **实现公共模块** (`common/`)：
   - `ApiResponse<T>` 统一响应封装类（字段：code, message, data, timestamp）
   - `BusinessException` 自定义业务异常类
   - `GlobalExceptionHandler` 全局异常处理（@RestControllerAdvice）

4. **实现 Health Check API**：
   - `GET /api/v1/health` 返回 `{ code: 200, message: "success", data: { status: "UP", version: "0.1.0" } }`
   - 创建 `HealthController`

5. **配置文件**：
   - `application.yml`：基础配置
   - `application-dev.yml`：开发环境（H2 内存数据库，端口 8080）
   - `application-prod.yml`：生产环境（PostgreSQL 连接，从环境变量读取）

6. **创建 Dockerfile**：
   - 多阶段构建（Maven build → JRE runtime）
   - 使用 eclipse-temurin:17-jre-alpine 作为运行镜像
   - JVM 参数：`-Xmx384m -Xms128m`
   - 暴露端口 8080

7. **创建基础单元测试**：
   - HealthController 的 MockMvc 测试

### Git 规范
- 在 develop 分支上创建 `feature/phase0-codex-backend-scaffold` 分支进行开发
- 使用 Conventional Commits：`feat(backend): ...`
- 完成后合并回 develop

### 约束
- 严格遵守 OWNERSHIP.md 权限。你不能修改 config/、model/、dto/ 目录（这些由架构师负责）
- Controller 统一返回 ApiResponse<T>
- 使用 @Slf4j 记录日志

### 交付标准
- [ ] `mvn clean package -DskipTests` 构建成功
- [ ] `mvn test` 测试通过
- [ ] Health API 在本地 `localhost:8080/api/v1/health` 可访问
- [ ] Dockerfile 可成功构建镜像
```

---

## 🎨 给 Gemini 的提示词（前端）

> 在 Gemini (如 Google AI Studio / Gemini CLI) 中 粘贴以下内容：

```
你是这个项目的前端开发者。请仔细阅读项目根目录下的 CONTRACT.md、OWNERSHIP.md 和 docs/API_CONTRACT.md，然后完成 Phase 0 前端脚手架搭建。

### 任务：[Phase 0] 前端脚手架搭建

### 上下文
- 本项目是一个 Spring Boot + Vue 3 的个人科研工具网站
- 你只负责前端代码（frontend/ 目录），严格遵守 OWNERSHIP.md 中的文件权限
- 接口定义参考 docs/API_CONTRACT.md

### 具体要求

1. **创建 Vue 3 项目**（在 `frontend/` 目录下）：
   - 使用 Vite 构建工具
   - TypeScript
   - 安装依赖：vue-router, pinia, axios, element-plus, @element-plus/icons-vue

2. **配置 Vite**（`vite.config.ts`）：
   - 开发服务器代理 `/api` 请求到 `http://localhost:8080`
   - Element Plus 按需自动导入（使用 unplugin-vue-components + unplugin-auto-import）

3. **创建项目目录结构**：
   ```
   frontend/src/
   ├── api/           # Axios 实例 + API 封装
   │   ├── request.ts # Axios 统一配置（baseURL, 拦截器, 错误处理）
   │   └── health.ts  # Health API 调用
   ├── assets/        # 静态资源
   ├── components/    # 公共组件
   ├── composables/   # 组合式函数
   ├── layouts/       # 布局组件
   │   └── DefaultLayout.vue  # 默认布局（侧边栏 + 顶栏 + 内容区）
   ├── pages/         # 页面
   │   └── home/
   │       └── index.vue  # 首页（暂时展示欢迎信息 + Health API 状态）
   ├── router/
   │   └── index.ts   # 路由配置
   ├── stores/        # Pinia Store
   ├── styles/
   │   └── index.css  # 全局样式
   ├── types/         # TypeScript 类型
   │   └── api.ts     # ApiResponse<T> 等通用类型
   ├── App.vue        # 根组件（使用 DefaultLayout）
   └── main.ts        # 入口（注册 Element Plus, Router, Pinia）
   ```

4. **DefaultLayout 布局**：
   - 使用 Element Plus 的 `el-container` + `el-aside` + `el-header` + `el-main`
   - 左侧边栏：导航菜单（首页、论文管理——先占位，后续 Phase 1 实现）
   - 顶部栏：网站标题「ChangYe 科研工具台」
   - 支持侧边栏折叠
   - 响应式设计

5. **首页 (Home)**：
   - 展示一个欢迎卡片
   - 调用 `GET /api/v1/health` 并展示服务状态（绿色 UP / 红色 DOWN）
   - 使用 Element Plus 的 `el-card`、`el-tag` 等组件

6. **Axios 封装** (`api/request.ts`)：
   - baseURL: `/api/v1`
   - 响应拦截器：自动解包 ApiResponse，错误时使用 ElMessage 提示
   - 请求超时: 15000ms

7. **创建 Dockerfile**：
   - 多阶段构建：node:18-alpine 构建 → nginx:alpine 运行
   - 将构建产物复制到 Nginx 默认目录

### 设计风格
- 整体风格：简洁专业的科研工具风格
- 配色：深蓝色系主色调（#1a1a2e → #16213e → #0f3460），白色内容区
- 侧边栏：深色背景
- 字体：系统默认，中英混排友好

### Git 规范
- 在 develop 分支上创建 `feature/phase0-gemini-frontend-scaffold` 分支进行开发
- 使用 Conventional Commits：`feat(frontend): ...`
- 完成后合并回 develop

### 约束
- 严格遵守 OWNERSHIP.md 权限，只能修改 frontend/ 目录下的文件
- 使用 <script setup lang="ts"> 组合式 API
- Element Plus 组件按需导入
- 所有页面组件必须有 data-testid 用于测试

### 交付标准
- [ ] `npm run dev` 可正常启动开发服务器
- [ ] 首页可正常展示，布局完整（侧边栏 + 顶栏 + 内容区）
- [ ] Health API 调用可正常发出请求（开发环境代理到后端）
- [ ] `npm run build` 构建成功无报错
- [ ] Dockerfile 可成功构建镜像
```

---

## ⚡ 下发顺序建议

```
1. 先给 Codex 和 Gemini 同时下发（两者完全独立，可并行）
2. 等两边都完成后，告诉我（Antigravity），我来：
   - 创建 model/、dto/、config/ 下架构师负责的文件
   - 编写 docker-compose.yml 和 Nginx 配置
   - 做集成联调
   - 合并到 develop 并打 v0.1.0 tag
```

> [!TIP]
> 如果 Codex/Gemini 遇到需要修改 config/model/dto 的情况，让它们告诉你，你再来找我处理。
