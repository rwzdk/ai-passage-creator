# 沅笺 · YuanJian Studio

<div align="center">

**YuanJian Studio 是一个面向图文内容创作的 AI 协作平台**

从选题、标题、大纲到正文与配图，提供可干预、可追踪、可继续编辑的完整创作流程。

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring AI Alibaba](https://img.shields.io/badge/Spring%20AI%20Alibaba-1.1.0-FF6A00?style=flat-square&logo=spring&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-21%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)

</div>

## 项目简介

YuanJian Studio 将多阶段 AI 创作流程封装为一个可视化工作台。用户可以先输入主题、受众、文章风格和创作要求，再逐步确认标题、调整大纲、生成正文、选择配图并完成图文合成，而不是一次性得到无法修改的结果。

项目当前公开仓库聚焦 **Java Spring Boot 后端 + Vue 3 前端**。本地存在的 Go、Python 后端目录不属于公开版本，已通过 Git 忽略规则排除，不会随远程仓库同步。

## ✨ 功能特性

### AI 分阶段创作

- **选题输入**：填写主题、创作要求、文章风格和配图策略。
- **标题生成**：AI 生成多个标题方案，用户确认后再进入下一阶段。
- **大纲生成**：根据主题和标题生成结构化大纲，支持手动编辑。
- **大纲优化**：可请求 AI 对当前大纲进行调整，而不必重新开始创作。
- **正文生成**：以流式方式生成 Markdown 正文，前端实时展示进度和内容。
- **图文合成**：分析正文中的配图需求，生成或检索图片后合并为完整文章。

### 实时过程与可编辑结果

- 使用 SSE 推送阶段状态、流式文本、配图状态、执行日志和错误信息。
- 创作过程中可以查看当前阶段、执行步骤和耗时。
- 支持文章正文编辑、历史文章查看、Markdown 导出、全文复制和重试创作。
- 文章详情页保留完整图文结果与智能体执行统计，方便复盘生成过程。

### 多策略配图

当前代码内置以下配图方式，具体可用项取决于配置的 API Key 和创作任务：

| 方式 | 用途 |
| --- | --- |
| Pexels | 根据关键词检索图库图片 |
| Nano Banana | 使用 Gemini 能力生成图片 |
| Nano Banana APICLAUDE | 通过兼容接口调用图片生成服务 |
| GPT Image 2 | 调用 GPT Image 2 生成图片 |
| Mermaid | 生成流程图、关系图等结构化图示 |
| Iconify | 检索并使用图标资源 |
| Emoji Pack | 检索适合作为内容插图的表情包 |
| SVG Diagram | 生成 SVG 概念示意图 |
| Picsum | 外部服务不可用时的随机图片降级方案 |

默认图库检索方式为 Pexels，默认 AI 生图方式为 Nano Banana，默认降级方式为 Picsum。

### 用户与运营能力

- 邮箱验证码注册、登录、会话管理和个人资料维护。
- 会员权益、创作配额和 Stripe 支付流程。
- 反馈建议邮件通知。
- 管理员用户管理、文章数据与创作执行统计。

## 文章风格

当前支持以下文章风格：

| 标识 | 说明 |
| --- | --- |
| `tech` | 科技风格，适合技术趋势、产品和行业分析 |
| `emotional` | 情感风格，强调情绪表达与共鸣 |
| `educational` | 教育风格，强调知识讲解、结构和可理解性 |
| `humorous` | 轻松幽默风格，强调口语化表达与阅读趣味 |

文章风格会参与正文生成提示词，最终内容仍会受到主题、受众和其他创作要求影响。

## 技术栈

### 后端

- Java 21、Spring Boot 3.5.9
- Spring AI Alibaba Agent Framework、DashScope
- MyBatis-Flex、MySQL 8、HikariCP
- Redis、Spring Session、Redisson
- Spring Mail、Stripe Java SDK、腾讯云 COS SDK
- Knife4j OpenAPI、PDFBox、Apache POI

### 前端

- Vue 3、TypeScript、Vite
- Ant Design Vue、Pinia、Vue Router、Axios
- ECharts、GSAP、Marked
- Nginx（Docker 生产容器）

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 22+
- MySQL 8+
- Redis 7+

### 1. 配置后端

复制示例配置并填写本地参数：

```powershell
Copy-Item .env.example .env
Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

至少需要配置 DashScope、Pexels、MySQL、Redis 和邮件服务。数据库初始化脚本位于 `sql/`；使用 Docker Compose 时会按编排文件自动挂载初始化脚本。

### 2. 启动后端

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

后端默认监听 `8123`，接口文档通常可通过 `http://localhost:8123/api/doc.html` 访问。

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端开发服务器默认地址为 `http://localhost:5173`，开发代理会将 `/api` 请求转发到后端。

## 🚀 线上部署

项目提供了基于 Docker Compose 的部署方案，包含 MySQL、Redis、Spring Boot 后端和 Nginx 前端四个服务。该配置适合部署到具备 Docker 环境的云服务器或其他 Linux 主机；不绑定特定云厂商。

### 部署步骤

```bash
git clone <你的仓库地址>
cd ai-passage-creator
cp .env.example .env
# 在 .env 中填写必需密钥和数据库密码
vim .env
docker compose up -d --build
```

Windows PowerShell 可使用：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

### 默认端口与健康检查

| 服务 | 默认端口 | 健康检查 |
| --- | ---: | --- |
| 前端 Nginx | `80` | `/health` |
| Spring Boot 后端 | `8123` | `/api/health/` |
| MySQL | 容器内部 `3306` | `mysqladmin ping` |
| Redis | 容器内部 `6379` | `redis-cli ping` |

MySQL 和 Redis 默认不暴露到宿主机，减少公网攻击面。如确需外部访问，请自行评估安全风险后再修改 `docker-compose.yml` 的端口映射。

### 部署维护

```bash
docker compose ps
docker compose logs -f backend
docker compose restart backend
git pull
docker compose up -d --build
docker compose down
```

首次初始化数据库由 MySQL 容器执行 `sql/` 中挂载的脚本。已有数据卷的环境不会因为普通重启重复初始化；执行 `docker compose down -v` 会删除数据库和 Redis 数据卷，请谨慎操作。

## 环境变量

完整配置项和默认值请以 `.env.example`、`docker-compose.yml` 以及 `application-local.yml.example` 为准。主要配置包括：

| 类别 | 变量示例 | 必需性 |
| --- | --- | --- |
| AI 文本 | `DASHSCOPE_API_KEY` | 必需 |
| 图片检索 | `PEXELS_API_KEY` | 必需 |
| 邮件 | `MAIL_USERNAME`、`MAIL_PASSWORD` | 必需（验证码与反馈） |
| 数据库 | `MYSQL_ROOT_PASSWORD`、`MYSQL_DATABASE` | 必需 |
| Redis | `REDIS_PASSWORD` | 可选 |
| AI 生图 | `NANO_BANANA_API_KEY`、`IMAGE_2_API_KEY` | 可选 |
| 会员支付 | `STRIPE_API_KEY`、`STRIPE_WEBHOOK_SECRET` | 可选 |
| 对象存储 | `TENCENT_COS_SECRET_ID`、`TENCENT_COS_SECRET_KEY` | 可选 |

请勿把真实密钥、邮箱授权码、Stripe Webhook Secret、COS 密钥或生产数据库密码写入仓库文件。

## 项目结构

```text
.
├── frontend/                         Vue 3 前端
│   └── src/
│       ├── api/                      接口请求
│       ├── components/               通用组件与动效
│       ├── pages/                    首页、创作、文章、用户、管理
│       ├── stores/                   Pinia 状态
│       └── utils/                    SSE 等工具
├── src/main/java/com/qc/template/
│   ├── agent/                        智能体、上下文与流程编排
│   ├── controller/                   HTTP 接口与鉴权入口
│   ├── service/                      文章、配图、用户、支付、统计
│   ├── mapper/                       MyBatis-Flex 数据访问
│   ├── model/                        DTO、实体、枚举、视图对象
│   └── config/                       模型、邮件、支付、外部服务配置
├── src/main/resources/               Spring 配置与映射文件
├── sql/                              建表与增量升级脚本
├── docs/                             VIP、支付等说明文档
├── docker-compose.yml                容器编排
├── Dockerfile                        后端镜像构建
└── pom.xml                           Maven 配置
```

更详细的请求链路、创作流程、SSE 通信和数据边界见 [项目架构概览](PROJECT_OVERVIEW.md)。

## 开发命令

```bash
./mvnw test
cd frontend
npm run type-check
npm run build
```

## 相关文档

- [项目架构概览](PROJECT_OVERVIEW.md)：模块边界、创作流程、实时通信与数据安全
- [VIP 功能说明](docs/VIP_FEATURES.md)：会员权益和配额相关说明
- [Stripe 支付配置](docs/STRIPE_SETUP.md)：支付参数与 Webhook 配置

## 安全与仓库边界

- `.env`、`application-local.yml` 等本地配置不会提交。
- Go、Python 后端目录仅保留在本地，不纳入公开仓库。
- 不提交数据库导出文件、运行时缓存、构建产物和临时测试文件。
- 提交前请检查 `git diff --cached`，确认没有密钥、个人账号或本地路径。
- 如果密钥曾经误提交，应立即在对应服务控制台撤销并重新生成，不能只依赖删除文件。

## 许可证

当前仓库未声明独立开源许可证。若要公开分发，请根据实际授权范围补充 LICENSE 文件。
