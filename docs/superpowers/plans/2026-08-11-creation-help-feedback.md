# 创作帮助与反馈 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在创作页提供使用帮助弹窗和可附图、可邮件发送的反馈功能。

**Architecture:** 新增独立的 Java `FeedbackService` 负责输入校验和邮件组装，`FeedbackController` 负责认证和 multipart 请求绑定。创作页通过既有请求客户端调用接口，保持帮助内容和反馈表单状态在页面内部。

**Tech Stack:** Spring Boot 3、Spring Mail、JUnit 5、Vue 3、TypeScript、Ant Design Vue。

## Global Constraints

- 收件邮箱固定为 `235173498@qq.com`。
- SMTP 授权信息只从 `MAIL_USERNAME`、`MAIL_PASSWORD` 读取，原始图片不落盘、不入库。
- 支持 JPG、JPEG、PNG、WEBP；最多 3 张，每张最多 5 MB；反馈内容 1 到 1,000 字符。

---

### Task 1: 反馈邮件服务

**Files:**
- Create: `src/main/java/com/qc/template/service/FeedbackService.java`
- Create: `src/main/java/com/qc/template/service/impl/FeedbackServiceImpl.java`
- Create: `src/test/java/com/qc/template/service/FeedbackServiceTest.java`
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/main/resources/application-local.yml.example`

**Interfaces:**
- Consumes: `User currentUser`, `String content`, `List<MultipartFile> images`。
- Produces: `void sendFeedback(User currentUser, String content, List<MultipartFile> images)`，非法输入抛出 `BusinessException`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void rejectsBlankFeedbackBeforeSendingMail() {
    assertThatThrownBy(() -> feedbackService.sendFeedback(user, "  ", List.of()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("反馈内容不能为空");
}
```

- [ ] **Step 2: 验证测试失败**

Run: `mvn -Dtest=FeedbackServiceTest test`

Expected: FAIL，因为 `FeedbackService` 尚不存在。

- [ ] **Step 3: 最小实现**

```java
public interface FeedbackService {
    void sendFeedback(User currentUser, String content, List<MultipartFile> images);
}
```

实现服务校验内容、文件数量、扩展名和大小；通过 `JavaMailSender` 创建带附件的 MIME 邮件，正文写入用户与反馈信息。加入 `spring-boot-starter-mail` 及 `feedback.mail.recipient`、`feedback.mail.sender-name` 和 Spring SMTP 环境变量配置。

- [ ] **Step 4: 验证测试通过**

Run: `mvn -Dtest=FeedbackServiceTest test`

Expected: PASS，覆盖空内容、非法文件和成功邮件正文/附件。

### Task 2: 已登录反馈接口

**Files:**
- Create: `src/main/java/com/qc/template/controller/FeedbackController.java`
- Modify: `src/test/java/com/qc/template/service/FeedbackServiceTest.java`

**Interfaces:**
- Consumes: `POST /api/feedback` 的 `content`、可选 `images` 和当前 `HttpServletRequest`。
- Produces: `BaseResponse<Boolean>`，成功数据为 `true`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void sendsAuthenticatedUsersFeedbackWithUploadedImages() {
    controller.submit("需要支持更多导出格式", List.of(image), request);
    verify(feedbackService).sendFeedback(currentUser, "需要支持更多导出格式", List.of(image));
}
```

- [ ] **Step 2: 验证测试失败**

Run: `mvn -Dtest=FeedbackServiceTest test`

Expected: FAIL，因为控制器提交方法尚不存在。

- [ ] **Step 3: 最小实现**

```java
@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<Boolean> submit(@RequestParam String content,
                                        @RequestParam(required = false) List<MultipartFile> images,
                                        HttpServletRequest request) {
        feedbackService.sendFeedback(userService.getLoginUser(request), content, images);
        return ResultUtils.success(true);
    }
}
```

- [ ] **Step 4: 验证测试通过**

Run: `mvn -Dtest=FeedbackServiceTest test`

Expected: PASS，接口从会话取得当前用户并委托服务。

### Task 3: 创作页帮助和反馈弹窗

**Files:**
- Modify: `frontend/src/api/articleController.ts`
- Modify: `frontend/src/pages/article/ArticleCreatePage.vue`

**Interfaces:**
- Consumes: 页脚“使用帮助”“反馈建议”点击事件。
- Produces: `submitFeedback(content: string, images: File[])` 请求 `POST /api/feedback`，帮助与反馈弹窗可打开/关闭。

- [ ] **Step 1: 写失败测试**

在现有前端测试框架可用时新增创作页组件测试，断言点击“使用帮助”显示第一个操作步骤，空反馈无法触发 `submitFeedback`；若项目未配置前端组件测试，记录此边界并以 TypeScript 构建和浏览器交互验证替代。

- [ ] **Step 2: 验证测试失败**

Run: `npm run type-check`

Expected: 当前页面没有帮助/反馈状态与 API 调用，新增测试或类型引用应失败。

- [ ] **Step 3: 最小实现**

```ts
export function submitFeedback(content: string, images: File[]) {
  const data = new FormData()
  data.append('content', content)
  images.forEach((image) => data.append('images', image))
  return request.post('/feedback', data)
}
```

在 `ArticleCreatePage.vue` 为两个入口添加按钮语义和点击事件；用 `a-modal` 设计帮助步骤和反馈表单；用 `a-upload` 校验文件类型、大小、数量，提交时禁用确认按钮，成功后清空并关闭。

- [ ] **Step 4: 验证实现**

Run: `npm run type-check`

Run: `npm run build`

Expected: PASS。

### Task 4: 集成检查

**Files:**
- Modify: `docs/superpowers/specs/2026-08-11-creation-help-feedback-design.md`（仅在实际约束变化时）

- [ ] **Step 1: 执行后端聚焦测试**

Run: `mvn -Dtest=FeedbackServiceTest test`

Expected: PASS。

- [ ] **Step 2: 静态检查**

Run: `git diff --check`

Expected: PASS。

- [ ] **Step 3: 浏览器检查**

打开创作页，验证两个入口、弹窗关闭、帮助文本、图片上传/移除、空表单校验和发送中的不可重复提交。
