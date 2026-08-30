// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取文章详情 GET /article/${param0} */
export async function getArticle(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getArticleParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseArticleVO>(`/article/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** AI 修改大纲 POST /article/ai-modify-outline */
export async function aiModifyOutline(
  body: API.ArticleAiModifyOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListOutlineSection>('/article/ai-modify-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 确认大纲 POST /article/confirm-outline */
export async function confirmOutline(
  body: API.ArticleConfirmOutlineRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-outline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 确认标题并输入补充描述 POST /article/confirm-title */
export async function confirmTitle(
  body: API.ArticleConfirmTitleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseVoid>('/article/confirm-title', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 创建文章任务 POST /article/create */
export async function createArticle(
  body: API.ArticleCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/article/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 启动文章标题生成 POST /article/start/${param0} */
export async function startArticle(
  params: { taskId: string },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>(`/article/start/${params.taskId}`, {
    method: 'POST',
    ...(options || {}),
  })
}

/** 保存文章编辑内容 POST /article/update-content */
export async function updateArticleContent(
  body: API.ArticleUpdateContentRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/article/update-content', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** AI 编辑文章内容 POST /article/ai-edit-content */
export async function aiEditArticleContent(
  body: API.ArticleAiEditRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString>('/article/ai-edit-content', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 重新生成文章配图 POST /article/regenerate-image */
export async function regenerateArticleImage(
  body: API.ArticleRegenerateImageRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseArticleVO>('/article/regenerate-image', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 切换文章配图版本 POST /article/select-image-version */
export async function selectArticleImageVersion(
  body: { taskId: string; position: number; versionId: string },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseArticleVO>('/article/select-image-version', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
    ...(options || {}),
  })
}

/** 上传并总结文章参考文档 POST /article/reference/parse */
export async function parseArticleReference(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('file', file)
  return request<API.BaseResponseDocumentReference>('/article/reference/parse', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}

/** 提交平台反馈建议 POST /feedback */
export async function submitFeedback(content: string, images: File[], options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('content', content)
  images.forEach((image) => formData.append('images', image))
  return request<API.BaseResponseBoolean>('/feedback', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}

/** 删除文章 POST /article/delete */
export async function deleteArticle(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/article/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 批量删除文章 POST /article/batch-delete */
export async function batchDeleteArticles(body: API.BatchDeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseInteger>('/article/batch-delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取任务执行日志 GET /article/execution-logs/${param0} */
export async function getExecutionLogs(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getExecutionLogsParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.BaseResponseAgentExecutionStats>(`/article/execution-logs/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 分页查询文章列表 POST /article/list */
export async function listArticle(body: API.ArticleQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageArticleVO>('/article/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取当前用户创作统计 GET /article/profile/stats */
export async function getUserArticleStats(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserArticleStatsVO>('/article/profile/stats', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取文章生成进度(SSE) GET /article/progress/${param0} */
export async function getProgress(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getProgressParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.SseEmitter>(`/article/progress/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
