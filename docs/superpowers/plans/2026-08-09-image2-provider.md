# Image2 Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `gpt-image-2` as a selectable article image provider through the apiclaude OpenAI-compatible API.

**Architecture:** Add an `IMAGE_2` strategy implementation beside `NanoBananaService`. The strategy sends prompt-only generation requests to `/v1/images/generations`, exposes a separate edit method for public image and mask URLs, then returns `ImageData` to the existing COS/fallback pipeline. The Vue article form adds the provider to the existing selection list.

**Tech Stack:** Spring Boot, OkHttp, Gson, Java JUnit, Vue 3, TypeScript, Ant Design Vue.

## Global Constraints

- Use `gpt-image-2` exactly as the provider model name.
- Generation requests must not include an `images` field.
- Edit image and mask values must be public direct image URLs.
- Preserve Nano Banana and existing fallback behavior.
- Do not add a standalone image editing page.

---

### Task 1: Add failing backend request tests

**Files:**
- Create: `src/test/java/com/qc/template/service/Image2ServiceTest.java`

- [ ] **Step 1: Write tests for prompt-only generation and URL-based edit payloads**

  Assert the service builds a generation request containing `model`, `prompt`, `size`, and `n`, without `images`; assert the edit request includes the source URL and optional mask URL.

- [ ] **Step 2: Run the focused test and confirm the expected failure**

  Run: `mvn -q -Dtest=Image2ServiceTest test`

  Expected: compilation/test failure because `Image2Service` and its request-building behavior do not exist.

### Task 2: Implement the Image2 backend strategy

**Files:**
- Create: `src/main/java/com/qc/template/config/Image2Config.java`
- Create: `src/main/java/com/qc/template/service/Image2Service.java`
- Modify: `src/main/java/com/qc/template/model/enums/ImageMethodEnum.java`
- Modify: `src/main/java/com/qc/template/service/ImageServiceStrategy.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-local.yml.example`
- Modify: `src/main/resources/application-prod.yml`

- [ ] **Step 1: Add the configuration properties and enum value**

  Bind `image-2` properties and add `IMAGE_2("IMAGE_2", "GPT Image 2 AI 生图", true, false)`. Make it available through the existing Spring service registration and use `image-2` as the COS folder.

- [ ] **Step 2: Implement prompt-only generation**

  POST to `${baseUrl}/v1/images/generations` with the configured model, prompt, size, and `n`; parse the first `data[0].url` or `data[0].b64_json` result into `ImageData`.

- [ ] **Step 3: Implement URL-based image editing**

  POST to `${baseUrl}/v1/images/edits` with `model`, `prompt`, `images: [{image_url: ...}]`, `size`, `n`, and optional `mask: {image_url: ...}`. Reject blank or non-HTTP(S) source/mask URLs before calling the provider.

- [ ] **Step 4: Run the focused tests and compile**

  Run: `mvn -q -Dtest=Image2ServiceTest test` and `mvn -q -DskipTests compile`.

### Task 3: Expose Image2 in the article frontend

**Files:**
- Modify: `frontend/src/pages/article/ArticleCreatePage.vue`
- Modify: `frontend/src/api/typings.d.ts`

- [ ] **Step 1: Add the failing type/build expectation**

  Update the API method documentation/type literal expectations to include `IMAGE_2`; run `pnpm --dir frontend type-check` and confirm the current frontend does not yet expose the new option.

- [ ] **Step 2: Add the selectable option and labels**

  Add an `IMAGE_2` checkbox alongside `NANO_BANANA`, preserving VIP gating and existing selection submission.

- [ ] **Step 3: Run frontend type-check and build**

  Run: `pnpm --dir frontend type-check` and `pnpm --dir frontend build`.

### Task 4: Final verification

**Files:**
- Verify: `git diff --check`

- [ ] **Step 1: Run focused backend and frontend verification**

  Run the Java focused test, Java compile, frontend type-check, frontend build, and `git diff --check`.

- [ ] **Step 2: Review the diff for secrets and scope**

  Confirm no API key is added to tracked configuration and only Image2 integration files changed.
