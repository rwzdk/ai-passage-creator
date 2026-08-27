<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import dayjs from 'dayjs'
import {
  ArrowRightOutlined,
  ClockCircleOutlined,
  EditOutlined,
  FileTextOutlined,
  OrderedListOutlined,
  PictureOutlined,
  RocketOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const topic = ref('')
const topicInputRef = ref<HTMLElement | null>(null)
const activePromptIndex = ref(0)
const promptIndex = ref(0)
const promptNoTransition = ref(false)
const promptPaused = ref(false)
const homeHotTopics = ref<API.HotTopicItem[]>([])
const hotTopicsSource = ref('fallback')
const hotTopicsRefreshing = ref(false)
const hotTopicsUpdatedAt = ref('')
let promptTimer: number | undefined
const promptCards = [
  { type: '工作总结', title: '总结一次项目复盘，提炼可复制的团队协作方法', description: '适合季度总结与项目复盘' },
  { type: '心得体会', title: '写一篇关于长期学习与自我成长的心得体会', description: '适合个人经历与观点表达' },
  { type: '演讲稿', title: '围绕人工智能时代的机会与挑战写一篇演讲稿', description: '适合分享、汇报与公开表达' },
  { type: '分析报告', title: '分析生成式 AI 对内容创作行业的影响', description: '适合行业观察与趋势分析' },
  { type: '爆款文章', title: '为什么越来越多人开始重新安排自己的生活节奏', description: '适合公众号和内容平台创作' },
  { type: '职场观察', title: '远程办公如何改变团队沟通与工作效率', description: '适合职场热点和经验分享' },
]
const displayPromptCards = computed(() => {
  if (homeHotTopics.value.length) {
    return homeHotTopics.value.map(item => ({
      type: '实时热点',
      title: item.title || '值得关注的今日话题',
      description: item.source || '来自 GNews 实时资讯',
    }))
  }
  return promptCards
})
const loopedPromptCards = computed(() => [...displayPromptCards.value, ...displayPromptCards.value])
const recentArticles = ref<API.ArticleVO[]>([])
const loadingArticles = ref(false)
const homePageRef = ref<HTMLElement | null>(null)
const homeMotionReady = ref(false)
const readyBackgrounds = ref<Record<string, boolean>>({})
let homeDataTimer: number | undefined
let backgroundObserver: IntersectionObserver | undefined
let revealObserver: IntersectionObserver | undefined

const metrics = computed(() => [
  { value: recentArticles.value.length, label: '最近作品', note: '已同步到作品库' },
  { value: 4, label: '创作步骤', note: '从选题到成稿' },
  { value: displayPromptCards.value.length, label: '热门选题', note: '点击即可开始创作' },
])

const features = [
  { icon: FileTextOutlined, index: '01', title: '从一个题目开始', description: '输入想表达的主题，先把模糊的灵感留在纸面上。' },
  { icon: OrderedListOutlined, index: '02', title: '让结构自然生长', description: 'AI 帮你梳理层次，让观点、情绪和素材彼此照应。' },
  { icon: EditOutlined, index: '03', title: '看正文流动起来', description: '创作过程实时展开，像看墨色沿着宣纸慢慢晕开。' },
  { icon: PictureOutlined, index: '04', title: '为文字留一束光', description: '为文章匹配合适的配图，让完整作品更有呼吸感。' },
]

const goToCreate = () => {
  const value = topic.value.trim()
  router.push(value ? { path: '/create', query: { topic: value } } : '/create')
}

const selectPrompt = (prompt: string) => {
  topic.value = prompt
  nextTick(() => topicInputRef.value?.querySelector('input')?.focus())
}

const nextPrompt = (manual = false) => {
  if ((!manual && promptPaused.value) || displayPromptCards.value.length === 0) return
  promptNoTransition.value = false
  promptIndex.value += 1
  if (promptIndex.value >= displayPromptCards.value.length) {
    window.setTimeout(() => {
      promptNoTransition.value = true
      promptIndex.value = 0
      requestAnimationFrame(() => { promptNoTransition.value = false })
    }, 520)
  }
  activePromptIndex.value = promptIndex.value % displayPromptCards.value.length
}

const previousPrompt = () => {
  if (displayPromptCards.value.length === 0) return
  if (promptIndex.value === 0) {
    promptNoTransition.value = true
    promptIndex.value = displayPromptCards.value.length
    requestAnimationFrame(() => { promptNoTransition.value = false })
  }
  promptIndex.value -= 1
  activePromptIndex.value = promptIndex.value % displayPromptCards.value.length
}

const startPromptRotation = () => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  promptTimer = window.setInterval(() => {
    nextPrompt()
  }, 3000)
}

const loadHomeHotTopics = async (refresh = false) => {
  if (refresh) hotTopicsRefreshing.value = true
  try {
    const { getHotTopics } = await import('@/api/hotTopicController')
    const res = await getHotTopics(refresh ? { params: { refresh: true } } : undefined)
    const data = res.data.data
    if (data?.items?.length) {
      homeHotTopics.value = data.items
      hotTopicsSource.value = data.source || 'gnews'
      hotTopicsUpdatedAt.value = data.updatedAt || new Date().toISOString()
      promptIndex.value = 0
      activePromptIndex.value = 0
    }
  } catch (error) {
    console.warn('首页热门选题加载失败，继续使用本地推荐', error)
  } finally {
    hotTopicsRefreshing.value = false
  }
}

const refreshHomeHotTopics = () => loadHomeHotTopics(true)

const formatHotTopicsTime = (value: string) => {
  if (!value) return ''
  return dayjs(value).format('HH:mm:ss')
}

const goToList = () => router.push('/article/list')
const viewArticle = (article: API.ArticleVO) => router.push(`/article/${article.taskId}`)

const loadRecentArticles = async () => {
  if (!loginUserStore.loginUser.id) return
  loadingArticles.value = true
  try {
    const { listArticle } = await import('@/api/articleController')
    const res = await listArticle({ pageNum: 1, pageSize: 6 })
    recentArticles.value = res.data.data?.records || []
  } catch (error) {
    console.error('加载最近作品失败:', error)
  } finally {
    loadingArticles.value = false
  }
}

const formatTime = (time: string | undefined) => (time ? dayjs(time).format('MM-DD HH:mm') : '--')
const statusText = (status?: string) => {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'PROCESSING') return '生成中'
  return '等待中'
}

const markBackgroundReady = (name: string) => {
  readyBackgrounds.value = { ...readyBackgrounds.value, [name]: true }
}

const observeHomeBackgrounds = () => {
  const sections = Array.from(
    homePageRef.value?.querySelectorAll<HTMLElement>('[data-lazy-background]') || [],
  )

  if (!sections.length) return
  if (!('IntersectionObserver' in window)) {
    sections.forEach(section => markBackgroundReady(section.dataset.lazyBackground || ''))
    return
  }

  backgroundObserver = new IntersectionObserver(
    entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return
        const section = entry.target as HTMLElement
        const name = section.dataset.lazyBackground
        if (name) markBackgroundReady(name)
        backgroundObserver?.unobserve(section)
      })
    },
    { rootMargin: '240px 0px' },
  )
  sections.forEach(section => backgroundObserver?.observe(section))
}

const observeHomeReveals = () => {
  revealObserver?.disconnect()
  const elements = Array.from(
    homePageRef.value?.querySelectorAll<HTMLElement>('[data-home-reveal]') || [],
  )
  if (!elements.length) return

  const showAll = () => elements.forEach(element => element.classList.add('is-home-revealed'))
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
    showAll()
    return
  }

  revealObserver = new IntersectionObserver(
    entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-home-revealed')
        revealObserver?.unobserve(entry.target)
      })
    },
    { rootMargin: '0px 0px -10% 0px', threshold: 0.12 },
  )
  elements.forEach(element => revealObserver?.observe(element))
}

watch(recentArticles, async () => {
  await nextTick()
  observeHomeReveals()
})

onMounted(() => {
  observeHomeBackgrounds()
  requestAnimationFrame(() => {
    homeMotionReady.value = true
    observeHomeReveals()
  })
  homeDataTimer = window.setTimeout(() => {
    void loadRecentArticles()
    void loadHomeHotTopics()
  }, 900)
  startPromptRotation()
})
onBeforeUnmount(() => {
  if (promptTimer) window.clearInterval(promptTimer)
  if (homeDataTimer) window.clearTimeout(homeDataTimer)
  backgroundObserver?.disconnect()
  revealObserver?.disconnect()
})
</script>

<template>
  <main id="homePage" ref="homePageRef" class="home-page" :class="{ 'is-home-motion-ready': homeMotionReady }">
    <section class="home-hero">
      <div class="hero-river" aria-hidden="true" />
      <div class="hero-mist hero-mist-one" aria-hidden="true" />
      <div class="hero-mist hero-mist-two" aria-hidden="true" />
      <div class="home-container hero-layout">
        <div class="hero-copy" data-hero-stage>
          <div class="eyebrow hero-eyebrow"><ThunderboltOutlined aria-hidden="true" /><span>AI 驱动的内容创作平台</span></div>
          <h1 class="hero-title">
            <span class="hero-title-line"><span class="hero-title-content">让每一次灵感</span></span>
            <span class="hero-title-line is-accent"><span class="hero-title-content">都留下清晰的回声</span></span>
          </h1>
          <p class="hero-subtitle">从一个选题出发，经过结构、正文与配图，写成属于你的完整作品。</p>
        </div>

        <div class="creation-journey" data-hero-stage>
          <div class="journey-heading">
            <span class="journey-kicker">创作路径</span>
            <strong>从灵感到成文</strong>
          </div>
          <ol class="journey-steps">
            <li class="journey-step is-active">
              <span class="journey-icon"><EditOutlined /></span>
              <div><strong>选题</strong><span>捕捉此刻想表达的内容</span></div>
            </li>
            <li class="journey-step">
              <span class="journey-icon"><OrderedListOutlined /></span>
              <div><strong>结构</strong><span>梳理观点与叙事的脉络</span></div>
            </li>
            <li class="journey-step">
              <span class="journey-icon"><FileTextOutlined /></span>
              <div><strong>正文</strong><span>让完整文章自然生长</span></div>
            </li>
          </ol>
        </div>

        <div class="topic-composer" data-hero-stage>
          <div class="composer-label"><span class="signal-dot" /> 今天，想写些什么？</div>
          <div class="composer-row">
            <a-input
              ref="topicInputRef"
              v-model:value="topic"
              class="topic-input"
              size="large"
              placeholder="例如：沅水边的清晨，如何写出一段安静的记忆"
              @pressEnter="goToCreate"
            >
              <template #prefix><EditOutlined /></template>
            </a-input>
            <a-button type="primary" size="large" class="cta-button" @click="goToCreate">
              <RocketOutlined /> 开始创作 <ArrowRightOutlined />
            </a-button>
          </div>
          <p class="composer-hint">工作总结、心得体会、演讲稿、分析报告，都可以从这里开始。</p>
          <div
            class="prompt-carousel"
            aria-label="实时创作主题"
            @mouseenter="promptPaused = true"
            @mouseleave="promptPaused = false"
            @focusin="promptPaused = true"
            @focusout="promptPaused = false"
          >
            <div class="prompt-carousel-heading">
              <span><span class="signal-dot" /> {{ hotTopicsSource === 'gnews' ? '正在发生的灵感' : '创作灵感推荐' }}</span>
              <span class="prompt-carousel-actions">
                  <span class="prompt-carousel-hint">点击主题即可填入<span v-if="hotTopicsUpdatedAt"> · {{ formatHotTopicsTime(hotTopicsUpdatedAt) }} 更新</span></span>
                  <a-button
                    type="text"
                    size="small"
                    class="prompt-refresh"
                    :loading="hotTopicsRefreshing"
                    aria-label="刷新实时热点"
                    @click="refreshHomeHotTopics"
                  >
                    <ReloadOutlined /> 刷新
                  </a-button>
              </span>
            </div>
            <button type="button" class="prompt-arrow prompt-arrow-left" aria-label="上一个主题" @click="previousPrompt">‹</button>
            <div class="prompt-viewport">
              <div
                class="prompt-track"
                :class="{ 'no-transition': promptNoTransition }"
                :style="{ '--prompt-index': promptIndex }"
              >
                <button
                  v-for="(prompt, index) in loopedPromptCards"
                  :key="`${prompt.title}-${index}`"
                  type="button"
                  :class="['prompt-card', { active: index % displayPromptCards.length === activePromptIndex }]"
                  @click="selectPrompt(prompt.title)"
                >
                  <span class="prompt-type">{{ prompt.type }}</span>
                  <strong>{{ prompt.title }}</strong>
                  <small>{{ prompt.description }}</small>
                </button>
              </div>
            </div>
            <button type="button" class="prompt-arrow prompt-arrow-right" aria-label="下一个主题" @click="nextPrompt(true)">›</button>
          </div>
        </div>
      </div>
    </section>

    <section
      class="method-section"
    >
      <div class="home-container">
        <div class="section-heading" data-home-reveal>
          <div class="eyebrow">A quiet writing room</div>
          <h2>让创作回到<br /><span>自然流动</span>的状态</h2>
          <p>不必一次想完整，不必从空白开始。沅笺把复杂的创作过程拆成四个可以被看见的小步骤。</p>
        </div>

        <div class="feature-grid" data-home-reveal>
          <article v-for="feature in features" :key="feature.index" class="feature-card">
            <div class="feature-topline"><span>{{ feature.index }}</span><component :is="feature.icon" /></div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <div class="feature-line" />
          </article>
        </div>
      </div>
    </section>

    <section
      class="metrics-section"
      data-lazy-background="metrics"
      :class="{ 'is-background-ready': readyBackgrounds.metrics }"
    >
      <div class="home-container metrics-layout">
        <div class="metrics-copy" data-home-reveal>
          <div class="eyebrow">Your creative trace</div>
          <h2 class="metrics-title">
            <span class="metrics-title-lead">每一次写下</span>
            <span class="metrics-title-main">都会成为你的创作历程</span>
          </h2>
          <p>登录后，你的作品、创作进度与写作痕迹会被整理在个人空间中，随时可以回望，也可以继续向前。</p>
          <a-button type="link" class="text-link" @click="router.push('/profile')">查看个人空间 <ArrowRightOutlined /></a-button>
        </div>

        <div class="metrics-grid" data-home-reveal>
          <article v-for="metric in metrics" :key="metric.label" class="metric-card">
            <div class="metric-value">{{ metric.value }}</div>
            <h3>{{ metric.label }}</h3>
            <p>{{ metric.note }}</p>
          </article>
        </div>
      </div>
    </section>

    <section
      v-if="loginUserStore.loginUser.id"
      class="recent-section"
      data-lazy-background="recent"
      :class="{ 'is-background-ready': readyBackgrounds.recent }"
    >
      <div class="home-container">
      <div class="section-heading compact-heading" data-home-reveal>
          <div class="recent-heading-copy">
            <div class="eyebrow"><span class="signal-dot" /> Recent notes</div>
            <h2>最近留下的作品</h2>
            <p>从这里继续你的下一段表达。</p>
          </div>
          <a-button type="link" class="text-link" @click="goToList">查看全部 <ArrowRightOutlined /></a-button>
      </div>

        <a-spin :spinning="loadingArticles">
          <div v-if="recentArticles.length" class="article-grid" data-home-reveal>
            <article v-for="article in recentArticles" :key="article.id" class="article-card" @click="viewArticle(article)">
              <div class="article-cover">
                <img
                  v-if="article.coverImage"
                  :src="article.coverImage"
                  :alt="article.mainTitle || article.topic"
                  width="640"
                  height="360"
                  loading="lazy"
                  decoding="async"
                />
                <FileTextOutlined v-else />
              </div>
              <div class="article-info">
                <h3>{{ article.mainTitle || article.topic || '未命名作品' }}</h3>
                <div class="article-meta">
                  <span><ClockCircleOutlined /> {{ formatTime(article.createTime) }}</span>
                  <span class="article-status">{{ statusText(article.status) }}</span>
                </div>
              </div>
            </article>
          </div>
          <div v-else-if="!loadingArticles" class="empty-articles">
            <FileTextOutlined />
            <strong>你的作品会从这里开始生长</strong>
            <span>写下第一个主题，建立属于自己的创作记录。</span>
            <a-button type="primary" @click="goToCreate">开始第一篇作品</a-button>
          </div>
        </a-spin>
      </div>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  overflow: hidden;
  background: var(--paper-warm);
}

.home-container {
  width: min(1160px, calc(100% - 48px));
  margin: 0 auto;
}

.home-hero {
  position: relative;
  min-height: 700px;
  overflow: hidden;
  background: linear-gradient(180deg, #dcebe2 0%, #eff5ef 56%, #f7f5ee 100%);
}

.hero-river {
  position: absolute;
  right: -8%;
  bottom: -30%;
  width: 76%;
  height: 74%;
  border: 1px solid rgba(69, 111, 100, 0.13);
  border-radius: 50% 48% 0 0;
  transform: rotate(-8deg);
  box-shadow: 0 -24px 80px rgba(143, 184, 164, 0.23);
}

.hero-river::after {
  position: absolute;
  inset: 12% 8% auto;
  height: 1px;
  background: rgba(69, 111, 100, 0.18);
  content: '';
  transform: rotate(3deg);
}

.hero-mist {
  position: absolute;
  width: 28rem;
  height: 15rem;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.34);
  filter: blur(12px);
}

.hero-mist-one { left: -8rem; top: 7rem; }
.hero-mist-two { right: 12rem; top: 16rem; animation-delay: -4s; }

.hero-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
  align-items: center;
  padding: 84px 0 72px;
}

.hero-copy { grid-column: 1 / -1; text-align: center; }

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--mountain-green);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-eyebrow {
  position: relative;
  display: inline-block;
}

.hero-eyebrow > .anticon {
  position: absolute;
  top: 50%;
  right: calc(100% + 8px);
  transform: translateY(-50%);
}

.hero-copy h1 {
  position: relative;
  overflow: hidden;
  margin: 24px 0 18px;
  color: var(--ink-deep);
  font-size: clamp(3.1rem, 5.6vw, 5.4rem);
  font-weight: 600;
  line-height: 0.98;
}

.hero-title-line {
  display: block;
  overflow: hidden;
  transform-origin: left bottom;
}

.hero-title-content {
  display: inline-block;
  transform-origin: left bottom;
  will-change: transform;
}

.hero-title-line.is-accent .hero-title-content {
  color: var(--mountain-green);
}

.hero-subtitle {
  max-width: 600px;
  margin: 0 auto;
  color: var(--color-text-secondary);
  font-size: 17px;
  line-height: 1.8;
}

.creation-journey { position: absolute; top: 132px; right: -12px; width: 220px; }
.journey-heading { display: grid; gap: 5px; }
.journey-kicker { color: var(--mountain-green); font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.journey-heading strong { color: var(--ink-deep); font-size: 18px; font-weight: 600; }
.journey-steps { display: grid; gap: 0; margin: 18px 0 0; padding: 0; list-style: none; }
.journey-step { position: relative; display: grid; grid-template-columns: 38px 1fr; gap: 12px; align-items: center; min-height: 60px; }
.journey-step:not(:last-child)::after { position: absolute; top: 46px; bottom: -7px; left: 18px; width: 1px; background: rgba(69, 111, 100, 0.24); content: ''; }
.journey-icon { z-index: 1; display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid rgba(69, 111, 100, 0.42); border-radius: 50%; background: rgba(69, 111, 100, 0.14); color: var(--mountain-green); font-size: 16px; }
.journey-step div { display: grid; gap: 3px; }
.journey-step strong { color: var(--ink-deep); font-size: 15px; font-weight: 600; }
.journey-step div span { color: var(--ink-muted); font-size: 11px; line-height: 1.45; }
.journey-step.is-active .journey-icon { border-color: var(--mountain-green); background: var(--mountain-green); color: #fff; box-shadow: 0 0 0 6px rgba(69, 111, 100, 0.14); }
.journey-step.is-active strong { color: var(--mountain-green); }
.journey-step.is-active div span { color: var(--color-text-secondary); }

.topic-composer {
  position: relative;
  grid-column: 1 / -1;
  width: min(920px, calc(100% - 96px));
  justify-self: center;
  margin: 38px auto 0;
  padding: 24px 26px;
  border: 1px solid rgba(69, 111, 100, 0.16);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: var(--shadow-lg);
}

.topic-composer::before {
  position: absolute;
  top: -1px;
  right: 12%;
  left: 12%;
  height: 1px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(69, 111, 100, .5), transparent);
  content: '';
  opacity: .55;
}

.composer-label { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: var(--ink-deep); font-size: 13px; font-weight: 600; }
.signal-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--mountain-green); box-shadow: 0 0 0 5px rgba(143, 184, 164, 0.24); }
.composer-row { display: flex; gap: 16px; }
.topic-input { flex: 1; border: 0 !important; box-shadow: none !important; background: rgba(255, 255, 255, 0.68) !important; }
.topic-input :deep(input) { height: 56px; font-size: 16px; }
.cta-button { display: inline-flex; align-items: center; gap: 8px; height: 56px; padding: 0 28px; white-space: nowrap; font-size: 15px; }
.composer-hint { margin: 12px 2px 0; color: var(--ink-muted); font-size: 13px; }
.prompt-carousel { --prompt-card-width: clamp(240px, 28vw, 300px); position: relative; margin-top: 18px; padding: 12px 34px 0; border-top: 1px solid rgba(69,111,100,.1); }
.prompt-carousel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; color: var(--ink-muted); font-size: 11px; }
.prompt-carousel-heading > span:first-child { display: inline-flex; align-items: center; gap: 7px; color: var(--ink-deep); font-weight: 600; }
.prompt-carousel-heading .signal-dot { width: 6px; height: 6px; box-shadow: 0 0 0 4px rgba(143,184,164,.16); }
.prompt-carousel-actions { display: inline-flex; align-items: center; gap: 8px; }
.prompt-carousel-hint { opacity: .72; }
.prompt-refresh { height: 24px; padding-inline: 6px; color: var(--mountain-green); font-size: 11px; }
.prompt-refresh:hover { color: var(--ink-deep); background: rgba(143,184,164,.12); }
.prompt-viewport { overflow: hidden; width: 100%; }
.prompt-track { display: flex; gap: 12px; width: max-content; transform: translate3d(calc(var(--prompt-index) * -1 * (var(--prompt-card-width) + 12px)), 0, 0); transition: transform 520ms cubic-bezier(.22,.61,.36,1); }
.prompt-track.no-transition { transition: none; }
.prompt-card { flex: 0 0 var(--prompt-card-width); min-height: 108px; padding: 14px; border: 1px solid rgba(69,111,100,.14); border-radius: 14px; background: rgba(247,250,246,.68); color: var(--ink-deep); text-align: left; cursor: pointer; transition: transform .35s ease, border-color .35s ease, background .35s ease, box-shadow .35s ease; }
.prompt-card:hover, .prompt-card.active { transform: translateY(-3px); border-color: rgba(69,111,100,.42); background: rgba(255,255,255,.92); box-shadow: 0 10px 24px rgba(48,79,70,.1); }
.prompt-type { display: block; margin-bottom: 8px; color: var(--mountain-green); font-size: 11px; font-weight: 700; letter-spacing: .08em; }
.prompt-card strong { display: -webkit-box; overflow: hidden; font-size: 13px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.prompt-card small { display: block; margin-top: 7px; color: var(--ink-muted); font-size: 11px; }
.prompt-arrow { position: absolute; top: 55%; z-index: 2; display: grid; width: 25px; height: 25px; place-items: center; padding: 0; border: 1px solid rgba(69,111,100,.16); border-radius: 50%; background: rgba(255,255,255,.75); color: var(--mountain-green); font-size: 22px; line-height: 1; cursor: pointer; transform: translateY(-50%); transition: background .25s ease, color .25s ease, transform .25s ease; }
.prompt-arrow:hover { background: var(--mountain-green); color: white; transform: translateY(-50%) scale(1.08); }
.prompt-arrow-left { left: 0; }
.prompt-arrow-right { right: 0; }

.method-section,
.recent-section { padding: 112px 0; background: var(--paper-warm); }

.method-section,
.metrics-section,
.recent-section {
  content-visibility: auto;
  contain-intrinsic-size: auto 680px;
}
.section-heading { max-width: 650px; margin-bottom: 46px; }
.section-heading h2,
.metrics-copy h2 { margin: 18px 0 14px; color: var(--ink-deep); font-size: clamp(2.4rem, 5vw, 4.6rem); font-weight: 500; line-height: 1.04; }
.section-heading h2 span { color: var(--mountain-green); }
.section-heading p,
.metrics-copy p { max-width: 520px; margin: 0; color: var(--color-text-secondary); font-size: 16px; line-height: 1.85; }
.feature-grid { position: relative; display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.feature-grid::before { position: absolute; top: 34px; right: 9%; left: 9%; height: 1px; background: linear-gradient(90deg, transparent, rgba(69,111,100,.42) 12%, rgba(199,168,120,.55) 50%, rgba(69,111,100,.42) 88%, transparent); content: ''; }
.feature-card { position: relative; z-index: 1; min-height: 276px; padding: 22px 24px 26px; overflow: hidden; border: 1px solid rgba(69,111,100,.12); border-radius: 8px; background: linear-gradient(145deg, rgba(255,255,255,.9), rgba(246,249,244,.76)); box-shadow: 0 12px 28px rgba(32,59,56,.055); transition: transform var(--transition-normal), border-color var(--transition-normal), box-shadow var(--transition-normal); }
.feature-card::before { position: absolute; top: 0; left: 0; width: 100%; height: 4px; background: var(--river-green); content: ''; transform: scaleX(.22); transform-origin: left; transition: transform var(--transition-normal); }
.feature-card::after { position: absolute; right: -30px; bottom: -42px; width: 116px; height: 116px; border: 1px solid rgba(69,111,100,.1); border-radius: 50%; content: ''; }
.feature-card:nth-child(2) { margin-top: 22px; background: linear-gradient(145deg, rgba(252,250,244,.94), rgba(246,249,244,.78)); }
.feature-card:nth-child(3) { margin-top: 8px; background: linear-gradient(145deg, rgba(247,251,248,.94), rgba(255,255,255,.76)); }
.feature-card:nth-child(4) { margin-top: 30px; background: linear-gradient(145deg, rgba(253,251,246,.94), rgba(244,249,245,.8)); }
.feature-card:hover { transform: translateY(-8px); border-color: rgba(69,111,100,.3); box-shadow: 0 22px 38px rgba(32,59,56,.13); }
.feature-card:hover::before { transform: scaleX(1); }
.feature-topline { display: flex; align-items: center; justify-content: space-between; color: var(--mountain-green); font-size: 18px; }
.feature-topline span { display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid rgba(69,111,100,.2); border-radius: 50%; background: rgba(255,255,255,.7); font-size: 12px; font-weight: 700; letter-spacing: .08em; box-shadow: 0 5px 12px rgba(32,59,56,.06); }
.feature-topline :deep(.anticon) { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 12px; background: rgba(143,184,164,.14); font-size: 17px; }
.feature-card h3 { position: relative; z-index: 1; margin: 38px 0 10px; color: var(--ink-deep); font-size: 20px; font-weight: 600; line-height: 1.35; }
.feature-card p { position: relative; z-index: 1; min-height: 68px; margin: 0; color: var(--ink-muted); font-size: 14px; line-height: 1.75; }
.feature-line { position: relative; z-index: 1; width: 52px; height: 2px; margin-top: 28px; background: linear-gradient(90deg, var(--river-green), rgba(143,184,164,.18)); }

.metrics-section { padding: 112px 0; background: linear-gradient(135deg, var(--ink-deep), #31574f); color: white; }
.metrics-layout { display: grid; grid-template-columns: 0.9fr 1.1fr; gap: 80px; align-items: center; }
.metrics-copy .eyebrow { color: var(--river-green); }
.metrics-copy h2 { color: white; }
.metrics-copy .metrics-title { display: grid; gap: 4px; font-size: clamp(2.2rem, 3.6vw, 4rem); line-height: 1.08; }
.metrics-title-lead,
.metrics-title-main { color: white; font-size: inherit; line-height: inherit; letter-spacing: 0; }
.metrics-copy p { color: rgba(243,247,243,0.72); }
.text-link { padding: 0; color: var(--river-green); font-weight: 700; }
.text-link:hover { color: white; }
.metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.metric-card { padding: 24px 18px; border: 1px solid rgba(255,255,255,0.15); border-radius: var(--radius-lg); background: rgba(255,255,255,0.08); backdrop-filter: blur(12px); }
.metric-value { color: #d5eadb; font-family: 'Outfit', sans-serif; font-size: 44px; font-weight: 600; line-height: 1; }
.metric-card h3 { margin: 18px 0 6px; color: white; font-size: 15px; }
.metric-card p { margin: 0; color: rgba(243,247,243,0.62); font-size: 12px; }

.compact-heading { display: flex; width: 100%; max-width: none; align-items: flex-end; justify-content: space-between; }
.compact-heading h2 { margin-bottom: 8px; font-size: clamp(2rem, 4vw, 3.4rem); }
.recent-heading-copy { position: relative; }
.recent-heading-copy::after { position: absolute; top: 4px; left: -18px; width: 1px; height: calc(100% - 8px); background: linear-gradient(180deg, transparent, rgba(69, 111, 100, .35), transparent); content: ''; }
.recent-section .signal-dot { display: inline-block; margin-right: 8px; vertical-align: middle; animation: signal-pulse 2s ease-in-out infinite; }
.article-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.article-card { position: relative; overflow: hidden; border: 1px solid var(--line-soft); border-radius: var(--radius-lg); background: rgba(255,255,255,0.72); cursor: pointer; transition: transform var(--transition-normal), box-shadow var(--transition-normal); }
.article-card::after { position: absolute; inset: 0; background: linear-gradient(115deg, transparent 25%, rgba(255,255,255,.28) 48%, transparent 72%); content: ''; pointer-events: none; transform: translateX(-130%); transition: transform 900ms var(--ease-out); }
.article-card:hover::after { transform: translateX(130%); }
.article-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-card-hover); }
.article-cover { display: grid; aspect-ratio: 16 / 9; place-items: center; overflow: hidden; background: var(--color-background-tertiary); color: var(--mountain-green); font-size: 32px; }
.article-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition-slow); }
.article-card:hover .article-cover img { transform: scale(1.05); }
.article-info { padding: 18px; }
.article-info h3 { display: -webkit-box; overflow: hidden; margin: 0 0 16px; color: var(--ink-deep); font-size: 17px; font-weight: 600; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--ink-muted); font-size: 12px; }
.article-meta span:first-child { display: inline-flex; align-items: center; gap: 5px; }
.article-status { display: inline-flex; align-items: center; gap: 6px; color: var(--mountain-green); }
.article-status::before { width: 6px; height: 6px; border-radius: 50%; background: var(--mountain-green); box-shadow: 0 0 0 4px rgba(143, 184, 164, .16); content: ''; }
.empty-articles { display: grid; justify-items: center; gap: 10px; padding: 58px 20px; border: 1px dashed var(--line-soft); border-radius: var(--radius-lg); color: var(--ink-muted); text-align: center; }
.empty-articles > :first-child { color: var(--river-green); font-size: 32px; }
.empty-articles strong { color: var(--ink-deep); }
.empty-articles span { margin-bottom: 8px; font-size: 13px; }

.home-page [data-home-reveal] {
  transition: opacity .48s ease, transform .48s ease;
}

.home-page.is-home-motion-ready [data-home-reveal]:not(.is-home-revealed) {
  opacity: 0;
  transform: translate3d(0, 24px, 0);
}

.home-page [data-home-reveal].is-home-revealed {
  opacity: 1;
  transform: none;
}

@keyframes mist-drift { from { transform: translate3d(-2%, 0, 0) scale(1); } to { transform: translate3d(4%, 10px, 0) scale(1.05); } }
@keyframes composer-breathe { 0%, 100% { opacity: .28; transform: scaleX(.9); } 50% { opacity: .85; transform: scaleX(1); } }

@media (max-width: 900px) {
  .hero-layout { gap: 24px; padding-top: 76px; }
  .creation-journey { position: relative; top: auto; right: auto; order: 2; justify-self: center; width: min(100%, 300px); }
  .topic-composer { width: 100%; order: 3; justify-self: center; margin-top: 0; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .feature-grid::before { right: 20%; left: 20%; }
  .feature-card:nth-child(2), .feature-card:nth-child(3), .feature-card:nth-child(4) { margin-top: 0; }
  .metrics-layout { grid-template-columns: 1fr; gap: 42px; }
}

@media (max-width: 640px) {
  .home-container { width: min(100% - 32px, 560px); }
  .home-hero { min-height: 700px; }
  .home-hero .hero-layout { padding: 76px 0 64px; }
  .hero-copy h1 { font-size: clamp(3rem, 15vw, 4.5rem); }
  .hero-subtitle { font-size: 15px; }
  .metrics-copy .metrics-title { font-size: clamp(1.9rem, 8.5vw, 3rem); }
  .composer-row { flex-direction: column; }
  .topic-composer { padding: 18px; }
  .prompt-carousel { --prompt-card-width: 78%; padding-inline: 30px; }
  .prompt-carousel-heading { align-items: flex-start; flex-direction: column; gap: 4px; }
  .topic-input :deep(input) { font-size: 14px; }
  .cta-button { justify-content: center; }
  .method-section, .metrics-section, .recent-section { padding: 76px 0; }
  .section-heading h2, .metrics-copy h2 { font-size: 2.7rem; }
  .feature-grid, .metrics-grid, .article-grid { grid-template-columns: 1fr; }
  .feature-grid::before { display: none; }
  .feature-card { min-height: auto; }
  .feature-card h3 { margin-top: 28px; }
  .compact-heading { align-items: start; flex-direction: column; gap: 12px; }
  .recent-heading-copy::after { display: none; }
}

@keyframes signal-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(143, 184, 164, .16); opacity: .72; }
  50% { box-shadow: 0 0 0 8px rgba(143, 184, 164, .04); opacity: 1; }
}
/* 新生成的沅水场景背景 */
.home-hero {
  background-image:
    linear-gradient(180deg, rgba(247, 250, 246, .18) 0%, rgba(247, 245, 238, .54) 92%),
    url('@/assets/scenes/home-river.webp');
  background-position: center;
  background-size: cover;
}

.home-hero::after {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: linear-gradient(90deg, rgba(247, 250, 246, .2), rgba(247, 250, 246, .04) 58%, rgba(32, 59, 56, .16));
  content: '';
  pointer-events: none;
}

.home-hero .hero-layout { position: relative; z-index: 1; }

@media (max-width: 768px) {
  .home-hero { background-attachment: scroll; background-position: 58% center; }
}

@media (prefers-reduced-motion: reduce) {
  .home-page [data-home-reveal] { transition: none; }
}

/* 首页每个分段使用独立场景，避免滚动后画面重复 */
.method-section {
  background-image:
    linear-gradient(180deg, rgba(247, 245, 238, .58), rgba(247, 250, 246, .54)),
    url('@/assets/scenes/home-method.webp');
  background-position: 20% 44%;
  background-size: cover;
}

.metrics-section.is-background-ready {
  background-image:
    linear-gradient(135deg, rgba(32, 59, 56, .94), rgba(49, 87, 79, .78)),
    url('@/assets/scenes/home-metrics.webp');
  background-position: 72% 60%;
  background-size: cover;
}

.recent-section.is-background-ready {
  background-image:
    linear-gradient(180deg, rgba(247, 245, 238, .54), rgba(247, 245, 238, .68)),
    url('@/assets/scenes/home-works.webp');
  background-position: 88% 78%;
  background-size: cover;
}

@media (max-width: 768px) {
  .method-section, .metrics-section, .recent-section { background-attachment: scroll; }
}
</style>
