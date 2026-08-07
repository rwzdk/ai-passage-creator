# 实时热门选题与首页主题卡片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** 将创作页固定热门选题替换为可缓存的 GNews 实时选题，并在首页创作输入框下方增加自动交替展示、点击回填的主题内容卡片。

**Architecture:** Java Spring Boot 后端通过独立的 `HotTopicService` 调用 GNews，按 10 分钟缓存结果并在异常时返回备用选题；Vue 前端新增热榜 API 封装，在创作页加载和手动刷新。首页只维护展示用的主题卡片状态，点击卡片复用现有 `topic` 输入和创作跳转流程。

**Tech Stack:** Spring Boot、Jackson、OkHttp、Vue 3、TypeScript、Ant Design Vue、现有 `ScrollReveal`/`StaggerList` 动效组件。

## Global Constraints

- 不修改文章生成、标题确认、大纲确认、正文流式生成和配图流程。
- GNews API Key 只保存在后端本地配置或环境变量，不进入前端代码和 Git。
- 外部服务失败、未配置 Key 或返回空数据时必须展示本地备用选题。
- 保留所有现有用户未提交改动；只提交本计划涉及的文件。
- 页面动画必须遵循 `prefers-reduced-motion`，刷新不能阻塞主题输入和开始创作。

---

### Task 1: 创建 GNews 配置与数据模型

**Files:**
- Create: `src/main/java/com/yupi/template/config/GNewsConfig.java`
- Create: `src/main/java/com/yupi/template/model/vo/HotTopicItemVO.java`
- Create: `src/main/java/com/yupi/template/model/vo/HotTopicsVO.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-local.yml`

**Interfaces:**
- `GNewsConfig` 提供 `apiKey`、`baseUrl`、`cacheMinutes`、`maxItems`。
- `HotTopicItemVO` 提供 `title`、`source`、`publishedAt`、`url`。
- `HotTopicsVO` 提供 `source`、`updatedAt`、`items`。

- [ ] 添加 `@ConfigurationProperties(prefix = "gnews")` 配置类，默认 `baseUrl=https://gnews.io/api/v4`、`cacheMinutes=10`、`maxItems=8`。
- [ ] 在 `application.yml` 添加安全默认值 `${GNEWS_API_KEY:}`，不写入真实密钥。
- [ ] 在 `application-local.yml` 添加同样的本地配置占位项，允许用户填入 Key。
- [ ] 运行 `mvn -DskipTests compile` 验证配置类和 VO 可编译。
- [ ] 提交 `feat: 增加实时热门选题配置模型`。

### Task 2: 实现后端热门选题服务与接口

**Files:**
- Create: `src/main/java/com/yupi/template/service/HotTopicService.java`
- Create: `src/main/java/com/yupi/template/service/impl/HotTopicServiceImpl.java`
- Modify: `src/main/java/com/yupi/template/controller/ArticleController.java`
- Test: `src/test/java/com/yupi/template/service/HotTopicServiceTest.java`

**Interfaces:**
- `HotTopicService#getHotTopics()` 返回 `HotTopicsVO`。
- `GET /article/hot-topics` 返回 `BaseResponse<HotTopicsVO>`。

- [ ] 添加集中维护的备用选题列表，包含 AI、职场、效率、学习、生活等主题。
- [ ] 使用现有 OkHttp 依赖请求 `${baseUrl}/top-headlines?category=general&lang=zh&country=cn&max=...&apikey=...`。
- [ ] 设置连接和读取超时；仅提取标题、来源、发布时间和 URL；过滤空标题和空 URL。
- [ ] 用线程安全内存缓存保存最近一次成功结果和更新时间，缓存有效时不重复请求外部服务。
- [ ] 外部请求失败、Key 为空、返回空数据或解析异常时返回备用数据，接口保持成功响应。
- [ ] 增加服务测试覆盖备用数据、缓存复用和正常数据映射；测试不得访问真实网络。
- [ ] 运行 `mvn -Dtest=HotTopicServiceTest test` 和 `mvn -DskipTests compile`。
- [ ] 提交 `feat: 接入实时热门选题服务`。

### Task 3: 接入前端热榜 API 与创作页

**Files:**
- Create: `frontend/src/api/hotTopicController.ts`
- Modify: `frontend/src/api/typings.d.ts`
- Modify: `frontend/src/pages/article/ArticleCreatePage.vue`

**Interfaces:**
- `getHotTopics()` 调用 `/article/hot-topics`。
- 前端类型 `HotTopicItem` 与 `HotTopicsVO` 字段对应后端返回值。

- [ ] 将固定 `exampleTopics` 改为 `fallbackTopics`，页面进入时请求实时热门选题。
- [ ] 增加 `hotTopics`、`hotTopicsLoading`、`hotTopicsUpdatedAt` 和 `hotTopicsSource` 状态。
- [ ] 热门区域增加刷新按钮和更新时间，加载时显示 `AnimatedSkeleton`，失败时不清空当前内容。
- [ ] 点击热门标题将标题写入当前选题输入框，不自动提交，用户仍可修改后开始创作。
- [ ] 运行 `npm run type-check`。
- [ ] 提交 `feat: 创作页展示实时热门选题`。

### Task 4: 增加首页交替主题卡片

**Files:**
- Modify: `frontend/src/pages/HomePage.vue`
- Modify: `frontend/src/components/motion/ScrollReveal.vue` only if required for replay behavior

**Interfaces:**
- 首页继续使用现有 `topic`、`goToCreate` 状态和函数。
- 主题卡片点击处理函数 `selectPrompt(prompt: string)` 将内容写入 `topic`。

- [ ] 在创作输入面板下方新增横向主题卡片轨道，使用 4-6 个完整创作主题，每个主题包含类型标签、完整题目和简短说明。
- [ ] 使用定时器每 4.5 秒切换一组卡片，切换采用左右交替滑入；鼠标悬停或键盘聚焦时暂停。
- [ ] 点击卡片只回填输入框并聚焦输入框，不直接开始任务；开始创作按钮仍由用户确认。
- [ ] 卡片使用现有项目颜色、玻璃面板、`StaggerList`/`ScrollReveal` 动效，移动端改为可横向滚动。
- [ ] 组件卸载时清理定时器；支持 `prefers-reduced-motion`，减少位移但保留内容切换。
- [ ] 运行 `npm run type-check`、`npm run build` 和 `git diff --check`。
- [ ] 提交 `feat: 增加首页动态创作主题卡片`。

### Task 5: 启动并浏览器验收

**Files:**
- No source changes unless verification finds a scoped issue.

- [ ] 启动 Java 后端和前端开发服务，确认 `http://localhost:5173/` 可访问。
- [ ] 浏览器检查首页主题卡片是否自动交替、悬停暂停、点击是否回填主题输入框。
- [ ] 打开创作页，检查热门选题首次加载、刷新、点击回填和备用数据展示。
- [ ] 直接访问 `/api/article/hot-topics`，确认响应结构和 `source` 字段。
- [ ] 检查浏览器控制台无本次功能新增错误。
- [ ] 若仅因未配置 GNews Key 返回备用数据，明确记录为配置状态，不误报为接口失败。
