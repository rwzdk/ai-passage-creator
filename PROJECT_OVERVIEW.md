# 项目架构概览

## 总体结构

YuanJian Studio 采用前后端分离结构：Vue 前端负责创作工作台和业务页面，Spring Boot 后端负责认证、文章流程、智能体调用、配图、支付和数据持久化。

```text
浏览器
  │
  ├── Vue 3 / Vite / Ant Design Vue
  │       ├── 创作工作台
  │       ├── 文章与个人中心
  │       └── 管理统计
  │
  └── HTTP + SSE
          │
          ▼
      Spring Boot API
          ├── Controller：接口与鉴权入口
          ├── Service：文章、用户、支付、配图和配额业务
          ├── Agent：标题、大纲、正文、配图分析与编排
          ├── Mapper：MyBatis-Flex 数据访问
          └── Config：模型、邮件、支付和外部服务
              ├── MySQL：用户、文章、支付和执行日志
              ├── Redis：会话、缓存和并发控制
              └── 外部 AI / 图片 / 支付服务
```

## 创作流程

1. 前端提交主题、文章风格和创作要求。
2. 后端创建创作状态，通过智能体编排生成标题方案。
3. 用户选择或编辑标题后，继续生成并调整文章大纲。
4. 后端流式生成正文，前端通过 SSE 更新阶段进度和内容。
5. 配图分析器提取图片需求，图片策略选择器调用对应服务。
6. 后端保存文章、图片版本和执行日志，前端展示可编辑的最终结果。

## 代码边界

- `frontend/src/pages/article/`：创作输入、标题、大纲、正文和完成状态。
- `src/main/java/com/qc/template/agent/`：智能体定义、上下文和流程编排。
- `src/main/java/com/qc/template/service/`：文章、图片、用户、支付和统计服务。
- `src/main/java/com/qc/template/controller/`：文章、用户、支付和统计 API。
- `src/main/resources/mapper/`：数据库映射文件。
- `sql/`：数据库初始化和结构升级脚本。

## 实时通信

创作过程使用 SSE 推送阶段事件、流式文本、执行日志、配图状态和错误信息。前端统一在 `frontend/src/utils/sse.ts` 处理连接、事件解析和异常状态，后端由文章创作服务负责产生事件。

## 数据与安全

业务数据存储在 MySQL，会话和部分临时状态使用 Redis。模型 Key、图片服务 Key、邮件授权码、Stripe 密钥和会话密钥通过本地配置或环境变量注入；`application-local.yml`、`.env` 等文件被 Git 忽略，不应上传到远程仓库。
