# 沅水青山前端视觉改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留现有业务流程和接口的前提下，将 YuanJian Studio 全部前端页面统一改造成“沅水青山、诗意清冷、水墨淡染”风格，并为滚动、加载、统计、表单和页面跳转加入稳定的动态反馈。

**Architecture:** 先建立全局设计 Token、页面过渡和可复用动效组件，再逐页接入。背景采用“场景绑定”模型，由页面场景控制预加载、交叉淡化和内容显示，避免单个卡片触发背景切换。每个页面独立验证并创建一个只包含该页面相关文件的 Git 提交。

**Tech Stack:** Vue 3、TypeScript、Vite、Vue Router、Pinia、Ant Design Vue、ECharts、CSS `IntersectionObserver`、`requestAnimationFrame`。

## Global Constraints

- 保留现有路由、登录状态、文章接口、支付接口和个人资料接口。
- 不在本次视觉改造中重写后端接口或数据库结构。
- 优先复用现有 Ant Design Vue 组件和项目已有背景资源。
- 动画不得阻塞首屏主要操作；按钮和表单在动画未完成时仍可用。
- 所有动画支持 `prefers-reduced-motion`。
- 背景图不包含文字、Logo、水印和高对比主体；中央或左侧保留文案空间。
- 验收尺寸为 375px、768px、1024px、1440px。
- 每个页面完成后运行页面级验证、`npm run type-check`、`npm run build`，再独立提交 Git。
- 不覆盖工作区内当前已经存在的其他用户改动。

---

### Task 1: 建立全局视觉 Token、页面过渡和动效基础

**Files:**
- Modify: `frontend/src/styles/variables.css`
- Modify: `frontend/src/styles/common.css`
- Modify: `frontend/src/layouts/BasicLayout.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/components/GlobalHeader.vue`
- Modify: `frontend/src/components/GlobalFooter.vue`
- Create: `frontend/src/components/motion/PageTransition.vue`
- Create: `frontend/src/components/motion/ScrollReveal.vue`
- Create: `frontend/src/components/motion/CountUpNumber.vue`
- Create: `frontend/src/components/motion/LazySection.vue`
- Create: `frontend/src/components/motion/SceneBackground.vue`
- Create: `frontend/src/components/motion/AnimatedSkeleton.vue`
- Create: `frontend/src/components/motion/StaggerList.vue`

**Interfaces:**
- `ScrollReveal` 接收 `visible?: boolean`、`delay?: number`，默认通过 `IntersectionObserver` 控制 `.is-visible`。
- `CountUpNumber` 接收 `value: number`、`duration?: number`、`suffix?: string`。
- `SceneBackground` 接收 `background: string`、`active: boolean`、`priority?: boolean`，只负责预加载和交叉淡化。
- 页面通过 `router-view` 使用统一的 `PageTransition`，业务页面不自行复制路由过渡逻辑。

- [ ] **Step 1: 更新 Token 和全局样式**

在 `variables.css` 增加墨色、山色、河水色、暖纸色、雾面玻璃、统一阴影、圆角、动效时长和断点；在 `common.css` 增加页面背景、按钮状态、面板、焦点态和减少动画规则。

- [ ] **Step 2: 创建通用动效组件**

所有进入动画只使用 `opacity` 和 `transform`；`CountUpNumber` 使用 `requestAnimationFrame`；`LazySection` 只控制显示状态，不负责请求；`SceneBackground` 使用双层背景交叉淡化。

- [ ] **Step 3: 接入全局布局和路由过渡**

将 `router-view` 包装为 `Transition`，统一 header/footer 的背景、层级和移动端折叠行为；确认登录页等全屏页面不被普通布局样式压缩。

- [ ] **Step 4: 验证并提交**

运行：

```powershell
cd E:\projects\ai-passage-creator\frontend
npm run type-check
npm run build
```

打开 `/`、`/user/login`、`/profile` 检查路由切换、键盘焦点和减少动画模式，然后提交：

```powershell
git add frontend/src/styles frontend/src/layouts/BasicLayout.vue frontend/src/App.vue frontend/src/components/GlobalHeader.vue frontend/src/components/GlobalFooter.vue frontend/src/components/motion
git commit -m "feat: 建立沅水青山全局视觉与动效基础"
```

### Task 2: 首页场景化改造

**Files:**
- Modify: `frontend/src/pages/HomePage.vue`
- Modify: `frontend/src/components/AuthBrandSection.vue`
- Modify: `frontend/src/assets/logo.png` only if a replacement asset is supplied

**Interfaces:**
- 使用 Task 1 的 `SceneBackground`、`ScrollReveal`、`CountUpNumber` 和 `StaggerList`。
- 首页继续使用现有登录用户状态和文章/统计接口，不新增后端请求。

- [ ] **Step 1: 将首页拆成首屏、创作流程、作品数据、行动区四个场景**
- [ ] **Step 2: 增加首屏背景优先加载和标题/副标题渐进出现**
- [ ] **Step 3: 增加创作流程滚动动画、统计数字动画和空数据状态**
- [ ] **Step 4: 在 375px、768px、1440px 检查布局与导航**
- [ ] **Step 5: 运行类型检查和构建并提交**

```powershell
git add frontend/src/pages/HomePage.vue frontend/src/components/AuthBrandSection.vue
git commit -m "feat: 重设计首页沅水场景与创作引导"
```

### Task 3: 登录页与注册页改造

**Files:**
- Modify: `frontend/src/pages/user/UserLoginPage.vue`
- Modify: `frontend/src/pages/user/UserRegisterPage.vue`

**Interfaces:**
- 保留现有表单字段、校验、登录和注册请求。
- 使用全局面板、按钮、错误状态和页面过渡，不修改 API 方法签名。

- [ ] **Step 1: 将登录和注册统一为清冷渡口场景**
- [ ] **Step 2: 增加表单分组渐入、提交 loading、错误反馈和成功跳转动画**
- [ ] **Step 3: 检查密码输入、键盘操作、窄屏布局和失败重试**
- [ ] **Step 4: 运行验证并提交**

```powershell
git add frontend/src/pages/user/UserLoginPage.vue frontend/src/pages/user/UserRegisterPage.vue
git commit -m "feat: 优化登录注册清冷渡口界面"
```

### Task 4: 文章创作流程改造

**Files:**
- Modify: `frontend/src/pages/article/ArticleCreatePage.vue`
- Modify: `frontend/src/pages/article/components/InputState.vue`
- Modify: `frontend/src/pages/article/components/TitleSelectingStage.vue`
- Modify: `frontend/src/pages/article/components/OutlineEditingStage.vue`
- Modify: `frontend/src/pages/article/components/CreatingState.vue`
- Modify: `frontend/src/pages/article/components/CompletedState.vue`

**Interfaces:**
- 保留现有创作状态机和 SSE/请求逻辑；视觉层只消费既有状态。
- 生成中状态必须继续支持取消、错误重试和完成后的操作按钮。

- [ ] **Step 1: 将创作流程设计成临水书桌和纸张场景**
- [ ] **Step 2: 优化步骤进度、输入区、生成状态和完成正文的动态进入**
- [ ] **Step 3: 让流式正文保持逐段显示，不等待整页完成才渲染**
- [ ] **Step 4: 检查长正文、失败状态和移动端输入区域**
- [ ] **Step 5: 运行验证并提交**

```powershell
git add frontend/src/pages/article/ArticleCreatePage.vue frontend/src/pages/article/components
git commit -m "feat: 优化文章创作流程与流式内容动效"
```

### Task 5: 作品列表和详情页改造

**Files:**
- Modify: `frontend/src/pages/article/ArticleListPage.vue`
- Modify: `frontend/src/pages/article/ArticleDetailPage.vue`
- Modify: `frontend/src/utils/article.ts`

**Interfaces:**
- 继续使用现有文章列表、详情和分页接口。
- 详情页正文保持完整展示，列表只显示摘要，避免影响阅读层级。

- [ ] **Step 1: 设计沿江作品浏览场景和列表层级**
- [ ] **Step 2: 增加骨架屏、错峰列表出现、分页/加载更多反馈**
- [ ] **Step 3: 增加详情页阅读进度、正文分段进入和返回列表状态保持**
- [ ] **Step 4: 检查空数据、接口错误、长标题和长正文**
- [ ] **Step 5: 运行验证并提交**

```powershell
git add frontend/src/pages/article/ArticleListPage.vue frontend/src/pages/article/ArticleDetailPage.vue frontend/src/utils/article.ts
git commit -m "feat: 优化作品浏览与正文阅读体验"
```

### Task 6: 个人资料页场景化改造

**Files:**
- Modify: `frontend/src/pages/user/UserProfilePage.vue`
- Modify: `frontend/src/assets/profile/profile-overview-bg.png` only if a replacement asset is supplied
- Modify: `frontend/src/assets/profile/my-works-bg.png` only if a replacement asset is supplied
- Modify: `frontend/src/assets/profile/edit-profile-bg.png` only if a replacement asset is supplied
- Modify: `frontend/src/assets/profile/creation-stats-bg.png` only if a replacement asset is supplied
- Modify: `frontend/src/assets/profile/creation-history-bg.png` only if a replacement asset is supplied

**Interfaces:**
- 保留现有五段导航、头像上传、资料编辑、作品分页、统计图表和时间线数据。
- 统一使用 Task 1 的场景背景和动效组件，背景只在完整分段进入阈值后切换。

- [ ] **Step 1: 重整五段内容和左侧导航的视觉层级**
- [ ] **Step 2: 修复背景与内容不同步，确保先背景后内容渐进加载**
- [ ] **Step 3: 优化创作数据、创作历程、作品正文和编辑资料面板**
- [ ] **Step 4: 检查单击导航、滚动导航、头像上传、资料保存和移动端折叠**
- [ ] **Step 5: 运行浏览器验证、类型检查和构建并提交**

```powershell
git add frontend/src/pages/user/UserProfilePage.vue frontend/src/assets/profile
git commit -m "feat: 重设计个人资料场景化内容"
```

### Task 7: VIP 支付页改造

**Files:**
- Modify: `frontend/src/pages/VipPage.vue`
- Modify: `frontend/src/api/paymentController.ts` only if the current UI response mapping needs correction

**Interfaces:**
- 保留现有 Stripe 支付跳转、支付状态和登录权限逻辑。
- 测试支付失败、取消返回和支付成功都必须有明确反馈。

- [ ] **Step 1: 设计夕照沅水和克制金色权益场景**
- [ ] **Step 2: 增加权益卡片渐入、套餐选择反馈和支付按钮状态**
- [ ] **Step 3: 检查未登录、支付跳转失败、取消支付和返回页面**
- [ ] **Step 4: 运行验证并提交**

```powershell
git add frontend/src/pages/VipPage.vue frontend/src/api/paymentController.ts
git commit -m "feat: 优化会员权益与支付交互界面"
```

### Task 8: 管理后台和统计页改造

**Files:**
- Modify: `frontend/src/pages/admin/UserManagePage.vue`
- Modify: `frontend/src/pages/admin/StatisticsPage.vue`
- Modify: `frontend/src/components/StatusBadge.vue`

**Interfaces:**
- 保留权限控制、用户管理接口和 ECharts 数据接口。
- 表格、筛选、图表、空数据和无权限状态必须使用统一状态样式。

- [ ] **Step 1: 使用低干扰的山脉水纹背景，不让背景影响表格阅读**
- [ ] **Step 2: 增加统计图表绘制、表格加载和筛选反馈**
- [ ] **Step 3: 检查无权限、空数据、请求失败和窄屏表格横向滚动**
- [ ] **Step 4: 运行验证并提交**

```powershell
git add frontend/src/pages/admin frontend/src/components/StatusBadge.vue
git commit -m "feat: 优化后台管理与数据统计界面"
```

### Task 9: 背景素材接入与全站体验验收

**Files:**
- Modify: `frontend/src/assets/profile/*` and other supplied background asset locations
- Modify: `frontend/src/styles/common.css` only for final responsive/motion fixes
- Create: `design-qa.md` only if an existing QA record needs updating

**Interfaces:**
- 只接入用户生成并提供的图片，不在代码中生成外部图片。
- 所有背景按场景映射表接入，并通过 `SceneBackground` 预加载。

- [ ] **Step 1: 替换并压缩用户提供的背景图**
- [ ] **Step 2: 启动前端和 Java 后端，打开首页、登录、创作、作品、个人资料、VIP、后台页面**
- [ ] **Step 3: 逐页检查 375px、768px、1024px、1440px**
- [ ] **Step 4: 检查页面跳转、滚动场景、加载/空数据/错误/权限、键盘和减少动画模式**
- [ ] **Step 5: 运行最终类型检查、构建和 `git diff --check`**
- [ ] **Step 6: 输出最终提交记录、未解决问题和背景图提示词**

```powershell
cd E:\projects\ai-passage-creator\frontend
npm run type-check
npm run build
git diff --check
```
