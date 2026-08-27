# YuanJian Studio

YuanJian Studio 是一个面向图文内容创作的 AI 协作平台。它将选题、标题、大纲、正文、配图和文章整理串成一个可交互的创作流程，用户可以在关键阶段查看结果、编辑内容并继续推进。

> 当前公开仓库包含 Java 后端和 Vue 前端。历史中的 Go、Python 后端实现已从公开仓库移除，并保留在本地作为被忽略的后端目录。

## 项目定位

- 以 Spring AI Alibaba 为基础实现多智能体协作
- 以阶段化流程支持标题选择、大纲编辑、正文生成和配图处理
- 通过 SSE 将创作进度、阶段结果和执行日志实时推送到前端
- 支持文章管理、用户资料、会员权益、支付和管理员统计
- 支持多种配图策略，包括图片搜索、AI 生图、Mermaid 图表和占位图兜底

## 技术栈

### 后端

| 技术 | 用途 |
| --- | --- |
| Java 21 | 运行环境 |
| Spring Boot 3.5.9 | Web 服务与依赖管理 |
| Spring AI Alibaba | 模型调用与智能体编排 |
| MyBatis-Flex | 数据访问 |
| MySQL | 业务数据持久化 |
| Redis / Spring Session / Redisson | 会话、缓存与并发控制 |
| Knife4j | OpenAPI 接口文档 |
| Spring Mail / Stripe | 邮件通知与会员支付 |

### 前端

| 技术 | 用途 |
| --- | --- |
| Vue 3 + TypeScript | 页面与交互 |
| Vite | 开发与构建 |
| Ant Design Vue | 业务组件 |
| Pinia | 状态管理 |
| Vue Router | 路由管理 |
| Axios | HTTP 请求 |
| ECharts | 管理统计图表 |
| GSAP | 页面动效 |
| Marked | Markdown 内容渲染 |

## 核心功能

1. 输入主题和创作要求，生成多个标题方案并由用户选择。
2. 生成文章大纲，支持人工编辑或使用 AI 优化。
3. 流式生成正文，并记录阶段状态和智能体执行日志。
4. 分析正文配图需求，选择图片来源并将结果插入文章。
5. 管理文章列表、详情、历史创作和个人资料。
6. 提供会员权益、Stripe 支付、配额管理和管理员统计。

## 快速开始

### 环境要求

- JDK 21+
- Node.js 22+
- MySQL 8+
- Redis 7+
- Maven 3.9+

### 配置

1. 复制 `.env.example` 为 `.env`，填写模型、图片服务、邮件和数据库配置。
2. 复制 `src/main/resources/application-local.yml.example` 为 `application-local.yml`，填写本地后端配置。
3. 前端默认通过 `/api` 访问后端；本地开发代理配置位于 `frontend/vite.config.ts`。
4. 初始化数据库时，按顺序执行 `sql/create_table.sql` 及所需的增量脚本。

本地配置文件和环境变量不会提交到仓库。请勿把真实 API Key、数据库密码、邮件授权码或支付密钥写入示例文件。

### 启动后端

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

后端默认端口为 `8123`。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认端口为 `5173`。

### Docker

配置 `.env` 后可使用：

```bash
docker compose up -d --build
```

## 项目结构

```text
.
├── frontend/                 Vue 前端
│   └── src/
│       ├── api/              接口请求
│       ├── components/       通用组件
│       ├── pages/            首页、创作、文章、用户和管理页面
│       ├── stores/            前端状态
│       └── utils/             SSE 等工具
├── src/main/java/com/qc/     Spring Boot 后端
│   ├── agent/                智能体与编排
│   ├── controller/           HTTP 接口
│   ├── service/              业务服务与配图策略
│   ├── mapper/               数据访问
│   ├── model/                DTO、实体和视图对象
│   └── config/               模型、支付和外部服务配置
├── src/main/resources/       Spring 配置和 MyBatis 映射
├── sql/                      数据库建表及增量脚本
├── docs/                     功能与设计文档
├── docker-compose.yml        本地容器编排
└── pom.xml                   Maven 配置
```

更详细的模块边界、请求链路和数据流见 [项目架构概览](PROJECT_OVERVIEW.md)。

## 开发命令

```bash
# 后端测试
./mvnw test

# 前端类型检查与构建
cd frontend
npm run type-check
npm run build
```

## 相关文档

- [项目架构概览](PROJECT_OVERVIEW.md)
- [VIP 功能说明](docs/VIP_FEATURES.md)
- [Stripe 支付配置](docs/STRIPE_SETUP.md)

## 安全说明

公开仓库只保存示例配置和源代码，不保存本地环境文件、真实密钥、数据库导出文件或运行时缓存。提交前请检查 `git diff --cached`，发现密钥泄露时应立即撤销并更换对应凭证。
