# 个人资料页作品收缩 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 移除资料页顶部绿色进度线，并让“我的作品”正文默认收起、可点击切换展开状态。

**Architecture:** 仅修改 `frontend/src/pages/user/UserProfilePage.vue`。使用组件内 `Set<string>` 保存当前展开作品的唯一键；模板仅在对应作品展开时渲染 Markdown 正文，页面刷新时状态自然重置。

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, SCSS, Ant Design Vue icons, marked。

## Global Constraints

- 不修改后端接口、数据模型或分页逻辑。
- 保留现有作品列表、加载、错误和跳转详情功能。
- 默认所有作品收起，刷新或重新进入页面后不持久化展开状态。

### Task 1: 修改资料页交互和样式

**Files:**
- Modify: `frontend/src/pages/user/UserProfilePage.vue`

**Interfaces:**
- Consumes: 现有 `articles` 列表与 `article.taskId || article.id` 唯一键。
- Produces: `toggleArticleExpanded(articleKey: string)` 和 `isArticleExpanded(articleKey: string)`，供作品模板使用。

- [ ] **Step 1: 移除顶部进度条节点与对应 CSS**

删除模板中的 `.profile-progress` 容器，并删除 `.profile-progress` 与 `.profile-progress-fill` 样式，避免生成任何固定绿色横线。

- [ ] **Step 2: 增加默认收起的展开状态**

在 `articles` 状态附近增加：

```ts
const expandedArticleKeys = ref(new Set<string>())

const getArticleKey = (article: API.ArticleVO) => String(article.taskId || article.id || '')

const isArticleExpanded = (article: API.ArticleVO) => expandedArticleKeys.value.has(getArticleKey(article))

const toggleArticleExpanded = (article: API.ArticleVO) => {
  const key = getArticleKey(article)
  const next = new Set(expandedArticleKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedArticleKeys.value = next
}
```

- [ ] **Step 3: 将作品标题改为展开/收起按钮**

让标题按钮调用 `toggleArticleExpanded(article)`，增加 `:aria-expanded="isArticleExpanded(article)"`；根据状态显示向上或向下箭头。正文改为 `v-if="isArticleExpanded(article)"`，这样默认只显示标题和元信息。

- [ ] **Step 4: 保持列表键逻辑一致**

模板 `v-for` 的 `:key` 与展开状态共用同一个 `getArticleKey(article)`，避免分页加载或重复标题导致状态错配。

- [ ] **Step 5: 运行聚焦验证**

运行：

```powershell
pnpm --dir frontend build:app
git diff --check
```

预期：构建成功且差异检查无输出；确认源码中不再存在 `.profile-progress` 节点，正文仅在展开条件成立时渲染。

- [ ] **Step 6: 提交实现**

```powershell
git add frontend/src/pages/user/UserProfilePage.vue
git commit -m "feat: collapse profile article bodies by default"
```
