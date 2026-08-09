# Image2 生图提供商设计

## 目标

在现有文章配图流程中增加 `gpt-image-2` 提供商，调用 `apiclaude.cc` 的 OpenAI 兼容接口，同时保留 Nano Banana、其他图片服务和 Picsum 降级逻辑。

## 范围

- Java 后端支持 `gpt-image-2` 文生图。
- Java 后端提供可复用的图改图请求方法，要求调用方传入公网图片 URL，可选公网蒙版 URL。
- 前端在文章创建页增加 `gpt-image-2` 配图方式，并将其纳入已有 `enabledImageMethods` 提交链路。
- 不新增独立图片编辑页面，不改变现有文章生成流程和权限模型。

## 方案

新增 `IMAGE_2` 枚举和 `Image2Service`。文生图使用 `POST /v1/images/generations`，请求只发送 `model`、`prompt`、`size`、`n`；图改图使用 `POST /v1/images/edits`，发送图片公网 URL 和可选蒙版 URL。服务返回 OpenAI 兼容响应中的第一张图片 URL 或 base64 图片，并交给现有 `ImageServiceStrategy` 统一上传 COS。

配置通过 `image-2.api-key`、`image-2.base-url`、`image-2.model`、`image-2.size`、`image-2.n` 注入，生产环境使用环境变量，不把密钥写入源码。

## 验证

- Java 单元测试验证文生图请求不带 `images`，图改图请求带图片 URL和蒙版 URL。
- Java 编译和聚焦测试通过。
- 前端运行类型检查和构建，确认新增枚举值、复选框和请求类型无误。
