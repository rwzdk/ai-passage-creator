# 项目架构概览

## 1. 总体结构

YuanJian Studio 采用前后端分离、阶段化任务和策略化配图架构。Vue 3 前端负责创作工作台、文章管理、用户中心和管理员页面；Spring Boot 后端负责鉴权、文章流程、AI 智能体、图片服务、会员支付和数据持久化。

```text
浏览器
  │
  ├── Vue 3 / Vite / Ant Design Vue
  │       ├── 首页、登录注册、个人资料
  │       ├── 创作工作台：输入、标题、大纲、正文、配图
  │       ├── 文章列表与详情：编辑、导出、执行日志
  │       └── 管理页面：用户与统计
  │
  └── HTTP + SSE
          │
          ▼
      Spring Boot API
          ├── Controller：接口、鉴权和参数入口
          ├── Service：文章、用户、配额、支付、反馈和统计
          ├── Agent：标题、大纲、正文、配图分析与编排
          ├── Manager：按 taskId 管理 SSE 连接
          ├── Mapper / Model：MyBatis-Flex 数据访问
          └── Config：AI、邮件、COS、Stripe 等外部服务
              ├── MySQL：用户、文章、支付记录、执行日志
              ├── Redis：会话、缓存和并发控制
              └── AI / 图片 / 支付服务
```

## 2. 代码边界

| 目录 | 职责 |
| --- | --- |
| `frontend/src/pages/article/` | 创作输入、标题选择、大纲编辑、正文生成和完成状态 |
| `frontend/src/api/` | 前端 API 请求与类型调用 |
| `frontend/src/utils/sse.ts` | SSE 建连、事件解析、断开与错误处理 |
| `src/main/java/com/qc/template/controller/` | HTTP 接口与鉴权入口 |
| `src/main/java/com/qc/template/service/` | 文章、用户、图片、支付、邮件和统计业务 |
| `src/main/java/com/qc/template/agent/` | 智能体、上下文、工具和流程编排 |
| `src/main/java/com/qc/template/manager/` | SSE 连接等运行时资源管理 |
| `src/main/java/com/qc/template/mapper/` | MyBatis-Flex Mapper |
| `src/main/resources/` | Spring 配置、Mapper XML 和提示词资源 |
| `sql/` | 建表脚本和数据库增量升级脚本 |

## 3. 文章创作流程

1. 前端提交主题、文章风格、创作要求、参考资料和配图配置。
2. 后端创建 taskId 与文章初始状态，并异步启动标题阶段。
3. 标题智能体生成多个方案，用户选择已有方案或填写自定义标题。
4. 大纲智能体生成结构化大纲，用户可以增删章节、编辑要点，或请求 AI 修改。
5. 正文智能体以流式方式生成 Markdown 正文，后端同步记录阶段状态。
6. 配图分析智能体提取封面和正文配图需求。
7. 配图策略选择器按配置并行调用图片检索、AI 生图或图表服务。
8. 图文合成阶段合并正文、图片和图片版本，保存文章与执行日志。
9. 前端进入完成状态，支持查看、编辑、复制、导出 Markdown 或重试。

## 4. 智能体与配图策略

文章智能体服务负责模型调用、提示词构建和阶段结果转换；异步阶段服务负责调度、状态更新、异常处理和 SSE 通知。配图策略使用统一的图片请求模型和方法枚举，当前包括 Pexels、Nano Banana、Nano Banana APICLAUDE、GPT Image 2、Mermaid、Iconify、Emoji Pack、SVG Diagram 和 Picsum 降级方案。

## 5. SSE 实时通信

客户端按 taskId 订阅创作进度。后端的 `SseEmitterManager` 使用并发 Map 保存连接，并负责创建、发送、完成、超时和异常清理。事件覆盖阶段状态、流式正文、配图状态、图文合成完成和错误信息；持久化的智能体日志用于详情页展示阶段名称、耗时和执行统计。

## 6. 数据、支付与安全

MySQL 保存用户、文章、文章状态、支付记录和智能体日志；Redis 用于登录会话、缓存和并发控制。Stripe 负责 VIP Checkout、Webhook 回调和退款流程；邮件服务负责注册验证码与反馈通知；腾讯云 COS 可用于图片对象存储。

所有密钥通过环境变量或本地配置注入。`.env`、`application-local.yml`、运行时数据、构建产物以及本地 Go、Python 后端目录不应提交到公开仓库。

## 7. Docker 部署

`docker-compose.yml` 编排 MySQL、Redis、backend 和 frontend 四个服务，并通过健康检查控制启动依赖。前端 Nginx 默认暴露 80 端口，后端默认暴露 8123 端口；MySQL 和 Redis 默认不映射宿主机端口。
