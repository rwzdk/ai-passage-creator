# 文档参考输入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让文章创作支持上传 PDF/Word/TXT/Markdown，并把后端生成的文件摘要作为标题、大纲和正文的参考输入。

**Architecture:** 新增无落盘的文档解析服务和 `/article/reference/parse` multipart 接口；前端保存接口返回的摘要，在 `/article/create` 传递 `referenceSummary`。文章实体保存摘要，异步阶段将其注入现有三类生成 prompt，未上传文件时请求和流程保持兼容。

**Tech Stack:** Spring Boot 3.5、Apache PDFBox、Apache POI、DashScope ChatModel、Vue 3 + TypeScript、Ant Design Vue。

## Global Constraints

- 支持 `.pdf`、`.doc`、`.docx`、`.txt`、`.md`，单文件最大 10 MB。
- 原始文件只在请求内存中处理，不写磁盘、COS 或数据库。
- 参考摘要最多 4,000 字符，并在 prompt 中作为不可信材料处理。
- 保留无文件时的现有 JSON 创建接口和创作流程。

---

### Task 1: 文档解析与摘要服务

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/qc/template/service/DocumentReferenceService.java`
- Create: `src/main/java/com/qc/template/model/vo/DocumentReferenceVO.java`
- Test: `src/test/java/com/qc/template/service/DocumentReferenceServiceTest.java`

**Interfaces:**
- `DocumentReferenceService.parse(MultipartFile file)` returns `DocumentReferenceVO`.
- `DocumentReferenceVO` contains `fileName`, `summary`, `characterCount`.

- [ ] **Step 1: Write failing tests** for supported extension validation, oversized input rejection, TXT extraction, and blank extracted text rejection.
- [ ] **Step 2: Run `mvn -q -Dtest=DocumentReferenceServiceTest test`** and confirm the tests fail because the service is missing.
- [ ] **Step 3: Add PDFBox and POI dependencies and implement in-memory extraction plus bounded summary generation.** Use `Loader.loadPDF`, `PDFTextStripper`, `XWPFDocument`, `HWPFDocument`, and UTF-8 text decoding; reject unsupported/empty input.
- [ ] **Step 4: Run the focused Maven test** and confirm it passes.

### Task 2: 创建任务持久化与三阶段 prompt

**Files:**
- Create: `sql/add_reference_summary.sql`
- Modify: `sql/create_table.sql`
- Modify: `src/main/java/com/qc/template/model/entity/Article.java`
- Modify: `src/main/java/com/qc/template/model/dto/article/ArticleCreateRequest.java`
- Modify: `src/main/java/com/qc/template/model/dto/article/ArticleState.java`
- Modify: `src/main/java/com/qc/template/service/ArticleService.java`
- Modify: `src/main/java/com/qc/template/service/impl/ArticleServiceImpl.java`
- Modify: `src/main/java/com/qc/template/service/ArticleAsyncService.java`
- Modify: `src/main/java/com/qc/template/agent/agents/TitleGeneratorAgent.java`
- Modify: `src/main/java/com/qc/template/agent/agents/OutlineGeneratorAgent.java`
- Modify: `src/main/java/com/qc/template/agent/agents/ContentGeneratorAgent.java`
- Modify: `src/main/java/com/qc/template/service/ArticleAgentService.java`
- Modify: `src/main/java/com/qc/template/agent/ArticleAgentOrchestrator.java`
- Modify: `src/main/java/com/qc/template/constant/PromptConstant.java`
- Modify: `src/main/java/com/qc/template/controller/ArticleController.java`

**Interfaces:**
- `ArticleCreateRequest.referenceSummary` is optional.
- Article creation service methods accept the optional summary.
- `ArticleState.referenceSummary` is propagated into all generation agents.

- [ ] **Step 1: Add the optional field and migration** with a `TEXT` column; keep existing callers compatible where possible.
- [ ] **Step 2: Add prompt reference sections and pass the saved summary into phase 1/2/3 state.** Empty summaries must produce no additional prompt section.
- [ ] **Step 3: Update controller/service calls** so the summary is saved before async phase 1 starts.
- [ ] **Step 4: Run `mvn -q -DskipTests compile`** and fix compile errors without changing unrelated behavior.

### Task 3: 上传接口与前端输入交互

**Files:**
- Modify: `src/main/java/com/qc/template/controller/ArticleController.java`
- Modify: `frontend/src/api/articleController.ts`
- Modify: `frontend/src/api/typings.d.ts`
- Modify: `frontend/src/pages/article/ArticleCreatePage.vue`

**Interfaces:**
- Frontend `parseArticleReference(file)` posts `FormData` to `/article/reference/parse`.
- Create request sends `referenceSummary` when present.

- [ ] **Step 1: Add the multipart controller endpoint** with login check and the service response.
- [ ] **Step 2: Add typed frontend API support** without manually setting the multipart boundary header.
- [ ] **Step 3: Add upload button, file input, loading/error/summary states, remove action, and create-button validation** while preserving the existing style/image controls.
- [ ] **Step 4: Run `npm run type-check`** and confirm the frontend types pass.

### Task 4: Full verification and git save

**Files:**
- Verify: changed source and migration files only.

- [ ] **Step 1: Run `mvn -q test` and `npm run build`.**
- [ ] **Step 2: Run `git diff --check` and inspect `git diff` for accidental unrelated changes.**
- [ ] **Step 3: Use the existing Chrome page at `http://localhost:5173/` to verify the input page and upload state without starting a quota-consuming generation.
- [ ] **Step 4: Stage only this feature's files and commit with `feat: support document reference input`.
