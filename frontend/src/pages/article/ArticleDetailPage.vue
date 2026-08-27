<template>
  <div class="article-detail-page">
    <div class="page-header">
      <div class="header-container">
        <div class="header-actions">
          <a-button @click="goBack" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回
          </a-button>
          <div class="right-actions">
            <a-button
              v-if="article?.status === 'FAILED'"
              type="primary"
              danger
              @click="handleRetry"
              class="retry-btn"
            >
              <template #icon>
                <RedoOutlined />
              </template>
              重新创建
            </a-button>
            <a-button v-if="article" @click="editArticle" class="edit-btn">
              <template #icon>
                <EditOutlined />
              </template>
              编辑
            </a-button>
            <a-button type="primary" @click="exportMarkdown" class="export-btn">
              <template #icon>
                <DownloadOutlined />
              </template>
              导出 Markdown
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading" tip="加载中...">
        <a-card :bordered="false" v-if="article" class="article-card">
          <!-- 标题 -->
          <div class="title-section">
            <h1 class="main-title">{{ article.mainTitle }}</h1>
            <p class="sub-title">{{ article.subTitle }}</p>
            <div class="meta-info">
              <a-tag :color="getStatusColor(article.status ?? '')" class="status-tag">
                {{ getStatusText(article.status ?? '') }}
              </a-tag>
              <span class="time">创建于 {{ article.createTime ? formatDate(article.createTime) : '' }}</span>
            </div>
          </div>

          <a-divider />

          <!-- 执行日志面板 -->
          <div v-if="visibleExecutionLogs.length > 0" class="execution-logs-section">
            <div class="logs-header" @click="showExecutionLogs = !showExecutionLogs">
              <h2 class="section-title">
                <ClockCircleOutlined class="section-icon" />
                执行日志
                <a-tag :color="getStatusColor(executionStats.overallStatus ?? '')" class="status-tag-small">
                  {{ executionStats.overallStatus ?? '' }}
                </a-tag>
              </h2>
              <ThunderboltOutlined :class="['toggle-icon', { expanded: showExecutionLogs }]" />
            </div>

            <Transition name="expand">
              <div v-show="showExecutionLogs" class="logs-content">
                <!-- 统计概览 -->
                <div class="stats-summary">
                  <div class="stat-item">
                    <span class="label">总耗时</span>
                    <span class="value">{{ formatDuration(executionStats.totalDurationMs) }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">智能体数量</span>
                    <span class="value">{{ visibleExecutionLogs.length }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">平均耗时</span>
                    <span class="value">
                      {{ formatDuration(visibleExecutionLogs.length ? Math.round(visibleTotalDurationMs / visibleExecutionLogs.length) : 0) }}
                    </span>
                  </div>
                </div>

                <!-- 智能体时间线 -->
                <div class="agent-timeline">
                  <div
                    v-for="log in visibleExecutionLogs"
                    :key="log.id"
                    :class="['timeline-item', log.status?.toLowerCase()]"
                  >
                    <div class="timeline-indicator">
                      <CheckCircleOutlined v-if="log.status === 'SUCCESS'" class="icon success" />
                      <CloseCircleOutlined v-else-if="log.status === 'FAILED'" class="icon failed" />
                      <LoadingOutlined v-else class="icon running" />
                    </div>
                    <div class="timeline-content">
                      <div class="timeline-header">
                        <span class="agent-name">{{ getAgentDisplayName(log.agentName ?? '') }}</span>
                        <span class="duration">{{ formatDuration(log.durationMs) }}</span>
                      </div>
                      <div class="timeline-time">
                        {{ log.startTime ? formatDate(log.startTime) : '' }}
                      </div>
                      <div v-if="log.errorMessage" class="error-message">
                        <CloseCircleOutlined /> {{ log.errorMessage }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <a-divider v-if="visibleExecutionLogs.length > 0" />

          <!-- 大纲 -->
          <div v-if="article.outline && article.outline.length > 0" class="outline-section">
            <h2 class="section-title">
              <OrderedListOutlined class="section-icon" />
              文章大纲
            </h2>
            <div class="outline-list">
              <div v-for="item in article.outline" :key="item.section" class="outline-item">
                <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                <ul class="outline-points">
                  <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                </ul>
              </div>
            </div>
          </div>

          <a-divider v-if="article.outline && article.outline.length > 0" />

          <!-- 完整图文（优先展示） -->
          <div v-if="article.fullContent" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              完整图文
            </h2>
            <div v-html="markdownToHtml(mergeArticleImages(article.fullContent, article.images))" class="markdown-content"></div>
          </div>

          <!-- 普通正文（无 fullContent 时展示） -->
          <div v-else-if="article.content" class="content-section">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              文章正文
            </h2>
            <div v-html="markdownToHtml(article.content)" class="markdown-content"></div>
          </div>

          <!-- 配图（仅在没有 fullContent 时单独展示） -->
          <div v-if="!article.fullContent && article.images && article.images.length > 0" class="images-section">
            <h2 class="section-title">
              <PictureOutlined class="section-icon" />
              文章配图
            </h2>
            <div class="images-grid">
              <div v-for="image in article.images" :key="image.position" class="image-item">
                <img :src="image.url" :alt="image.description" />
                <div class="image-info">
                  <span class="badge">{{ image.method }}</span>
                  <span class="keywords">{{ image.keywords }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  FileTextOutlined,
  PictureOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  RedoOutlined,
  EditOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import { getArticle, getExecutionLogs } from '@/api/articleController'
import { mergeArticleImages } from '@/utils/article'
import { marked } from 'marked'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref<API.ArticleVO | null>(null)
const executionStats = ref<API.AgentExecutionStats | null>(null)
const logsLoading = ref(false)
const showExecutionLogs = ref(false)

// Markdown 转 HTML
const markdownToHtml = (markdown: string) => {
  return marked(markdown)
}

// 加载文章
const loadArticle = async () => {
  const taskId = route.params.taskId as string
  if (!taskId) {
    message.error('文章ID不存在')
    return
  }

  loading.value = true
  try {
    const res = await getArticle({ taskId })
    article.value = res.data.data || null
    // 自动加载执行日志
    await loadExecutionLogs(taskId)
  } catch (error) {
    message.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 加载执行日志
const loadExecutionLogs = async (taskId: string) => {
  logsLoading.value = true
  try {
    const res = await getExecutionLogs({ taskId })
    executionStats.value = res.data.data || null
  } catch (error) {
    console.error('加载执行日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

// 导出 Markdown
const exportMarkdown = () => {
  if (!article.value) return

  let markdown = `# ${article.value.mainTitle}\n\n`
  markdown += `> ${article.value.subTitle}\n\n`

  // 优先使用完整图文
  if (article.value.fullContent) {
    markdown += mergeArticleImages(article.value.fullContent, article.value.images)
  } else {
    if (article.value.outline && article.value.outline.length > 0) {
      markdown += `## 目录\n\n`
      article.value.outline.forEach(item => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }

    markdown += article.value.content || ''

    const bodyImages = article.value.images?.filter((image) => image.position !== 1 && image.url)
    if (bodyImages && bodyImages.length > 0) {
      markdown += `\n\n## 配图\n\n`
      bodyImages.forEach(image => {
        markdown += `![${image.description || image.sectionTitle || '文章配图'}](${image.url})\n\n`
      })
    }
  }

  const blob = new Blob([markdown], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${article.value.mainTitle}.md`
  a.click()
  URL.revokeObjectURL(url)

  message.success('导出成功')
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const formatDuration = (durationMs?: number) => {
  return `${((durationMs ?? 0) / 1000).toFixed(1)}s`
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    PENDING: 'default',
    PROCESSING: 'processing',
    COMPLETED: 'success',
    FAILED: 'error',
  }
  return colorMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return textMap[status] || status
}

// 仅展示用户可理解的关键步骤，隐藏 SSE 回放等内部事件。
const agentDisplayNames: Record<string, string> = {
    'agent1_generate_titles': '生成标题',
    'agent2_generate_outline': '生成大纲',
    'agent3_generate_content': '生成正文',
    'agent4_analyze_image_requirements': '分析配图需求',
    'agent5_generate_images': '生成配图',
    'agent6_merge_content': '图文合成',
    'ai_modify_outline': 'AI修改大纲'
}

const visibleExecutionLogs = computed(() => {
  return (executionStats.value?.logs ?? []).filter((log) => Boolean(log.agentName && agentDisplayNames[log.agentName]))
})

const visibleTotalDurationMs = computed(() => {
  return visibleExecutionLogs.value.reduce((total, log) => total + (log.durationMs ?? 0), 0)
})

const getAgentDisplayName = (agentName: string) => {
  return agentDisplayNames[agentName] ?? ''
}

// 重试（重新创建文章）
const handleRetry = () => {
  if (!article.value) return

  Modal.confirm({
    title: '确认重试',
    content: '将使用相同的选题和配置重新创建文章，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      router.push({
        path: '/create',
        query: {
          topic: article.value?.topic
        }
      })
    }
  })
}

const editArticle = () => {
  const taskId = article.value?.taskId || route.params.taskId
  if (!taskId) return

  router.push({
    path: '/create',
    query: { taskId: String(taskId) },
  })
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-detail-page {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 12px 20px 16px;
    margin-bottom: 16px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    box-sizing: border-box;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .right-actions {
    display: flex;
    gap: 12px;
  }

  .back-btn {
    height: 36px;
    padding: 0 12px;
    background: transparent;
    border-color: transparent;
    color: var(--color-text);
    font-size: 13px;
    font-weight: 600;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      background: rgba(143, 184, 164, 0.15);
      border-color: transparent;
      color: var(--color-text);
    }
  }

  .retry-btn {
    height: 36px;
    padding: 0 12px;
    background: #ff4d4f;
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .edit-btn {
    height: 36px;
    padding: 0 12px;
    border-color: transparent;
    background: transparent;
    color: var(--color-text);
    font-weight: 600;
    font-size: 13px;
    border-radius: var(--radius-md);

    &:hover {
      border-color: transparent;
      background: rgba(143, 184, 164, 0.15);
      color: var(--color-text);
    }
  }

  .export-btn {
    height: 36px;
    padding: 0 14px;
    background: var(--gradient-primary);
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-green);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .article-card {
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    background: white;

    :deep(.ant-card-body) {
      padding: 40px;
    }
  }

  .title-section {
    margin-bottom: 28px;
    text-align: center;

    .main-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 10px;
      color: var(--color-text);
      line-height: 1.3;
      letter-spacing: -0.5px;
    }

    .sub-title {
      font-size: 16px;
      color: var(--color-text-secondary);
      margin: 0 0 20px;
    }

    .meta-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      color: var(--color-text-muted);
      font-size: 13px;
    }

    .status-tag {
      border-radius: var(--radius-full);
      font-size: 12px;
      padding: 2px 12px;
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--color-text);
  }

  .section-icon {
    font-size: 18px;
    color: var(--color-text-secondary);
  }

  .status-tag-small {
    font-size: 11px;
    padding: 2px 8px;
    margin-left: 8px;
  }

  /* 执行日志部分 */
  .execution-logs-section {
    margin-bottom: 28px;
    background: var(--color-background-secondary);
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    overflow: hidden;

    .logs-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background var(--transition-fast);

      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      .section-title {
        margin: 0;
        display: flex;
        align-items: center;
      }

      .toggle-icon {
        font-size: 14px;
        color: var(--color-text-secondary);
        transition: transform var(--transition-fast);

        &.expanded {
          transform: rotate(180deg);
        }
      }
    }

    .logs-content {
      padding: 0 20px 20px;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
      padding: 16px;
      background: white;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border-light);

      .stat-item {
        text-align: center;

        .label {
          display: block;
          font-size: 12px;
          color: var(--color-text-muted);
          margin-bottom: 4px;
        }

        .value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }

    .agent-timeline {
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 16px;
        top: 12px;
        bottom: 12px;
        width: 2px;
        background: var(--color-border);
      }

      .timeline-item {
        position: relative;
        padding-left: 48px;
        padding-bottom: 20px;

        &:last-child {
          padding-bottom: 0;
        }

        .timeline-indicator {
          position: absolute;
          left: 8px;
          top: 2px;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--color-border);

          .icon {
            font-size: 12px;

            &.success {
              color: var(--color-success);
            }

            &.failed {
              color: var(--color-error);
            }

            &.running {
              color: var(--color-primary);
            }
          }
        }

        &.success .timeline-indicator {
          border-color: var(--color-success);
        }

        &.failed .timeline-indicator {
          border-color: var(--color-error);
        }

        .timeline-content {
          background: white;
          padding: 12px 16px;
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-light);

          .timeline-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .agent-name {
              font-size: 14px;
              font-weight: 600;
              color: var(--color-text);
            }

            .duration {
              font-size: 13px;
              font-weight: 600;
              color: var(--color-primary);
            }
          }

          .timeline-time {
            font-size: 12px;
            color: var(--color-text-muted);
          }

          .error-message {
            margin-top: 8px;
            padding: 8px;
            background: rgba(255, 77, 79, 0.1);
            border-radius: var(--radius-md);
            font-size: 12px;
            color: var(--color-error);
            display: flex;
            align-items: flex-start;
            gap: 6px;

            .anticon {
              flex-shrink: 0;
              margin-top: 2px;
            }
          }
        }
      }
    }
  }

  /* 展开/收起动画 */
  .expand-enter-active,
  .expand-leave-active {
    transition: all 0.3s ease;
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    opacity: 0;
    max-height: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    opacity: 1;
    max-height: 2000px;
  }

  .outline-section {
    margin-bottom: 28px;

    .outline-list {
      .outline-item {
        margin-bottom: 12px;
        padding: 16px;
        background: var(--color-background-secondary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border-light);
        transition: all var(--transition-fast);

        &:hover {
          border-color: var(--color-border);
        }

        .outline-title {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
          color: var(--color-text);
        }

        .outline-points {
          margin: 0;
          padding-left: 18px;

          li {
            margin-bottom: 4px;
            color: var(--color-text-secondary);
            line-height: 1.6;
            font-size: 13px;
          }
        }
      }
    }
  }

  .content-section {
    margin-bottom: 28px;

    .markdown-content {
      line-height: 1.8;
      font-size: 15px;
      color: var(--color-text);

      :deep(h2) {
        font-size: 20px;
        font-weight: 600;
        margin: 28px 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--color-border);
        color: var(--color-text);
      }

      :deep(h3) {
        font-size: 17px;
        font-weight: 600;
        margin: 22px 0 10px;
        color: var(--color-text);
      }

      :deep(p) {
        margin-bottom: 14px;
        text-indent: 2em;
        color: var(--color-text);
      }

      :deep(ul), :deep(ol) {
        margin-bottom: 14px;
        padding-left: 2em;
      }

      :deep(li) {
        margin-bottom: 6px;
        color: var(--color-text);
      }

      :deep(img) {
        display: block;
        max-width: 100%;
        max-height: 600px;
        width: auto;
        height: auto;
        margin: 20px auto;
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-md);
        object-fit: contain;
      }

      // Mermaid 图表特殊处理（SVG 格式）
      :deep(img[src$=".svg"]) {
        max-width: 800px;
        max-height: 500px;
      }
    }
  }

  .images-section {
    .images-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;

      .image-item {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--color-border);
        transition: all var(--transition-normal);
        cursor: pointer;

        &:hover {
          border-color: var(--color-text-muted);
          box-shadow: var(--shadow-md);
        }

        img {
          width: 100%;
          height: 160px;
          object-fit: cover;
        }

        .image-info {
          padding: 12px;
          background: white;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .badge {
            padding: 3px 10px;
            background: var(--color-text);
            color: white;
            border-radius: var(--radius-md);
            font-size: 11px;
            font-weight: 500;
          }

          .keywords {
            font-size: 11px;
            color: var(--color-text-muted);
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .article-detail-page {
    .page-header {
      padding: 8px 12px 16px;
    }

    .header-container {
      padding: 0;
    }

    .article-card {
      :deep(.ant-card-body) {
        padding: 24px;
      }
    }

    .title-section {
      .main-title {
        font-size: 22px;
      }

      .sub-title {
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 520px) {
  .article-detail-page {
    .header-actions {
      align-items: stretch;
      flex-wrap: wrap;
    }

    .back-btn {
      width: auto;
    }

    .right-actions {
      flex: 1 1 100%;
      gap: 8px;
    }

    .retry-btn {
      flex-basis: 100%;
    }

    .right-actions :deep(.ant-btn) {
      flex: 1 1 0;
    }
  }
}
/* 沿江阅读页视觉层 */
.article-detail-page {
  background:
    radial-gradient(circle at 84% 4%, rgba(143, 184, 164, 0.2), transparent 35rem),
    var(--paper-warm);
}

.article-detail-page .page-header { background: transparent; }
.article-detail-page .article-card { border-color: var(--line-soft); background: rgba(255, 255, 255, 0.8); box-shadow: var(--shadow-xl); backdrop-filter: blur(15px); }
.article-detail-page .article-card :deep(.ant-card-body) { padding: clamp(28px, 5vw, 64px); }
.article-detail-page .title-section { padding-bottom: 26px; border-bottom: 1px solid var(--line-soft); }
.article-detail-page .title-section .main-title { color: var(--ink-deep); font-family: var(--font-display); font-size: clamp(2rem, 4vw, 3.6rem); font-weight: 500; }
.article-detail-page .content-section .markdown-content { max-width: 760px; margin: 0 auto; font-size: 16px; line-height: 2; }
.article-detail-page .content-section .markdown-content :deep(p) { color: #34504a; }
.article-detail-page .outline-section .outline-list .outline-item { border-color: var(--line-soft); border-left: 3px solid var(--river-green); background: rgba(243, 247, 243, 0.65); }
.article-detail-page .execution-logs-section { border-color: var(--line-soft); background: rgba(243, 247, 243, 0.65); }
.article-detail-page .images-section .images-grid .image-item { border-color: var(--line-soft); }
.article-detail-page .images-section .images-grid .image-item:hover { border-color: var(--river-green); box-shadow: var(--shadow-card-hover); transform: translateY(-4px); }
.article-detail-page .content-section, .article-detail-page .outline-section, .article-detail-page .execution-logs-section { animation: reading-section-in 520ms var(--ease-out) both; }

@keyframes reading-section-in { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
/* 雾中正文阅读背景 */
.article-detail-page {
  background-image:
    linear-gradient(180deg, rgba(247, 250, 246, .58), rgba(247, 245, 238, .9) 78%),
    url('@/assets/scenes/article-detail-mist.webp');
  background-position: center top;
  background-size: cover;
  background-attachment: fixed;
}

.article-detail-page .article-card { background: rgba(255, 255, 255, .78); }

@media (max-width: 768px) {
  .article-detail-page { background-attachment: scroll; background-position: center top; }
}
</style>
