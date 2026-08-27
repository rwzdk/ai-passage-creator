# Personal Profile Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an animated personal profile page with editable contact information, avatar upload, user creation/admin visibility, and a lazy-loaded section that renders the user's complete article bodies.

**Architecture:** Keep the existing Spring Boot + MyBatis-Flex API as the source of truth. Extend the existing `user` record with optional profile fields, reuse the existing COS upload service for avatars, and expose a small authenticated article-statistics endpoint. The Vue page will use the existing `loginUser` store, article pagination API, `marked`, and `IntersectionObserver`; no new frontend dependency is required.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Flex, MySQL, Tencent COS, Vue 3, TypeScript, Vue Router, Ant Design Vue, SCSS, marked.

---

### Task 1: Add profile fields and persistence migration

**Files:**
- Create: `sql/add_user_profile_fields.sql`
- Modify: `sql/create_table.sql`
- Modify: `src/main/java/com/qc/template/model/entity/User.java`
- Modify: `src/main/java/com/qc/template/model/dto/user/UserRegisterRequest.java`
- Modify: `src/main/java/com/qc/template/model/dto/user/UserAddRequest.java`
- Modify: `src/main/java/com/qc/template/model/dto/user/UserUpdateRequest.java`
- Modify: `src/main/java/com/qc/template/model/vo/LoginUserVO.java`
- Modify: `src/main/java/com/qc/template/model/vo/UserVO.java`

- [ ] Add nullable `userEmail`, `userPhone`, `userBlog`, and `userGithub` fields with bounded varchar columns.
- [ ] Add the same optional fields to registration/admin update DTOs and both sanitized VO classes; never expose `userPassword`.
- [ ] Add an idempotent migration using `ADD COLUMN IF NOT EXISTS` compatible with the project's MySQL setup, and include the fields in the baseline table definition.
- [ ] Run `mvn -q -DskipTests compile` after the model changes.

### Task 2: Implement authenticated profile update and avatar upload

**Files:**
- Create: `src/main/java/com/qc/template/model/dto/user/UserProfileUpdateRequest.java`
- Modify: `src/main/java/com/qc/template/service/UserService.java`
- Modify: `src/main/java/com/qc/template/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/qc/template/controller/UserController.java`

- [ ] Add a dedicated profile update request containing nickname, introduction, email, phone, blog, and GitHub URL.
- [ ] Validate lengths and basic email/URL/phone formats, preserve omitted values, update only the authenticated user's row, and return refreshed `LoginUserVO` data.
- [ ] Add `POST /user/profile/update` for the current session.
- [ ] Add authenticated `POST /user/avatar/upload` using `MultipartFile`, reject non-image files and files over 5 MB, call the existing `CosService.uploadBytes`, persist the returned URL, and return refreshed login-user data.
- [ ] Keep admin `/user/update` and `/user/add` compatible with the new optional fields.
- [ ] Add focused service tests for valid profile updates, invalid email/URL, and unauthenticated access.

### Task 3: Add per-user article statistics

**Files:**
- Create: `src/main/java/com/qc/template/model/vo/UserArticleStatsVO.java`
- Modify: `src/main/java/com/qc/template/controller/ArticleController.java`
- Modify: `src/main/java/com/qc/template/service/ArticleService.java`
- Modify: `src/main/java/com/qc/template/service/impl/ArticleServiceImpl.java`

- [ ] Add `GET /article/profile/stats` behind the existing login check.
- [ ] Return total works, completed works, latest creation time, and total body character count for the authenticated user only.
- [ ] Count logical non-deleted records and treat null content as zero characters.
- [ ] Add a focused service test that proves another user's articles are excluded.

### Task 4: Add frontend API types and profile route

**Files:**
- Modify: `frontend/src/api/typings.d.ts`
- Modify: `frontend/src/api/userController.ts`
- Modify: `frontend/src/api/articleController.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/GlobalHeader.vue`

- [ ] Add TypeScript types for profile fields, profile update response, avatar upload response, and article stats.
- [ ] Add `updateMyProfile`, `uploadAvatar`, and `getUserArticleStats` API wrappers; use `FormData` for avatar upload.
- [ ] Add lazy route `/profile` and a “个人资料” dropdown action from the authenticated header.
- [ ] Keep the existing top navigation unchanged; the profile page is entered from the user menu and its internal right-side buttons scroll to sections.

### Task 5: Build the animated profile and complete-work sections

**Files:**
- Create: `frontend/src/pages/user/UserProfilePage.vue`
- Modify: `frontend/src/stores/loginUser.ts` only if the existing store needs a typed refresh helper

- [ ] Build sections for hero/profile, contact details, animated statistics, creation timeline, and “我的作品”.
- [ ] Add a compact right-side section menu with `scrollIntoView` targets for statistics, creation history, works, and edit profile.
- [ ] Use an `IntersectionObserver` to reveal each section only when it enters the viewport; show a loading skeleton before the delayed reveal.
- [ ] Start all statistic counters at zero and animate them to server values; honor `prefers-reduced-motion`.
- [ ] Add avatar picker preview, file validation, upload progress state, and store refresh after success.
- [ ] Add an edit form for nickname, introduction, QQ email, phone, blog, and GitHub; show inline validation and refresh the store after saving.
- [ ] Load articles page-by-page when the works section enters view and when its sentinel is reached. Render each article's `fullContent` first, falling back to `content`, through `marked`; show empty, loading, and error states.
- [ ] Preserve full article body text and metadata; clicking the article heading may open the existing `/article/:taskId` detail page in a new route.
- [ ] Keep the existing green visual system, responsive behavior at 375/768/1024/1440 widths, and avoid a required external background image.

### Task 6: Synchronize registration and admin user management

**Files:**
- Modify: `frontend/src/pages/user/UserRegisterPage.vue`
- Modify: `frontend/src/pages/admin/UserManagePage.vue`
- Modify: `src/main/java/com/qc/template/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/qc/template/controller/UserController.java`

- [ ] Add optional nickname, QQ email, and phone fields to registration without making existing account/password flows invalid.
- [ ] Persist those values during registration using the same validation rules as profile editing.
- [ ] Display email, phone, blog, and GitHub in the admin table with compact responsive cells.
- [ ] Preserve existing admin-only access and delete behavior.

### Task 7: Verify build, API, and rendered browser behavior

**Files:**
- No additional source files unless verification exposes a defect.

- [ ] Run `mvn -q test` and `npm run build` from `frontend`.
- [ ] Apply `sql/add_user_profile_fields.sql` to the configured local database and verify the new columns exist.
- [ ] Start the Java backend and existing frontend; verify `GET /user/get/login`, profile save, avatar upload, article stats, and paginated article loading with an authenticated browser session.
- [ ] Test the profile page at 375px, 768px, 1024px, and 1440px widths; verify internal jumps, reveal animations, zero-to-value counters, reduced-motion behavior, full article rendering, and empty/error states.
- [ ] Run `git diff --check` and report any remaining environment-dependent limitation, especially missing COS credentials or empty article data.

