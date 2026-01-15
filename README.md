# AI 爆款文章创作器

基于 Spring AI Alibaba 构建的智能文章生成系统，支持从选题到图文文章的全自动生成。

## 项目介绍

通过 5 个智能体协作完成文章创作：
1. **智能体1**：生成标题
2. **智能体2**：生成大纲  
3. **智能体3**：生成正文（流式输出）
4. **智能体4**：配图需求分析
5. **智能体5**：配图生成（Pexels 图库检索）

## 技术栈

**后端**：Spring Boot 3.5 + Spring AI Alibaba 1.1.0 + MyBatis-Flex + MySQL

**前端**：Vue 3 + TypeScript + Ant Design Vue + Vite

## 快速开始

### 1. 数据库初始化

```bash
mysql -uroot -p < sql/create_table.sql
```

### 2. 配置 API Key

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填写：
- 通义千问 API Key（必填）
- Pexels API Key（必填）

### 3. 启动后端

```bash
mvn spring-boot:run
```

访问接口文档：http://localhost:8567/api/doc.html

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问前端页面：http://localhost:5173

## 核心功能

- ✅ 智能标题生成
- ✅ 自动大纲规划
- ✅ 流式正文创作
- ✅ 智能配图检索
- ✅ 实时进度展示（SSE）
- ✅ 文章管理（列表、详情、删除）
- ✅ Markdown 导出

## 作者

<a href="https://codefather.cn">编程导航学习圈</a>
