<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { listArticle } from '@/api/articleController'
import dayjs from 'dayjs'
import {
  ArrowRightOutlined,
  ClockCircleOutlined,
  EditOutlined,
  FileTextOutlined,
  OrderedListOutlined,
  PictureOutlined,
  RocketOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import ScrollReveal from '@/components/motion/ScrollReveal.vue'
import CountUpNumber from '@/components/motion/CountUpNumber.vue'
import StaggerList from '@/components/motion/StaggerList.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const topic = ref('')
const recentArticles = ref<API.ArticleVO[]>([])
const loadingArticles = ref(false)

const metrics = computed(() => [
  { value: recentArticles.value.length, label: '最近作品', note: '已同步到作品库' },
  { value: 4, label: '创作步骤', note: '从选题到成稿' },
  { value: 1, label: '实时输出', note: '支持流式生成正文' },
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

const goToList = () => router.push('/article/list')
const viewArticle = (article: API.ArticleVO) => router.push(`/article/${article.taskId}`)

const loadRecentArticles = async () => {
  if (!loginUserStore.loginUser.id) return
  loadingArticles.value = true
  try {
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

onMounted(loadRecentArticles)
</script>

<template>
  <main id="homePage" class="home-page">
    <section class="home-hero">
      <div class="hero-river" aria-hidden="true" />
      <div class="hero-mist hero-mist-one" aria-hidden="true" />
      <div class="hero-mist hero-mist-two" aria-hidden="true" />
      <div class="home-container hero-layout">
        <ScrollReveal :once="true" class="hero-copy">
          <div class="eyebrow"><ThunderboltOutlined /> AI 驱动的内容创作平台</div>
          <h1>让每一次灵感，<br /><em>都留下清晰的回声</em></h1>
          <p class="hero-subtitle">从一个选题出发，经过结构、正文与配图，写成属于你的完整作品。</p>
        </ScrollReveal>

        <ScrollReveal :once="true" :delay="120" class="creation-orbit">
          <div class="orbit-ring orbit-ring-outer" />
          <div class="orbit-ring orbit-ring-inner" />
          <div class="orbit-core"><EditOutlined /></div>
          <span class="orbit-label orbit-label-top">选题</span>
          <span class="orbit-label orbit-label-right">结构</span>
          <span class="orbit-label orbit-label-bottom">正文</span>
        </ScrollReveal>

        <ScrollReveal :once="true" :delay="220" class="topic-composer">
          <div class="composer-label"><span class="signal-dot" /> 今天，想写些什么？</div>
          <div class="composer-row">
            <a-input
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
        </ScrollReveal>
      </div>
    </section>

    <section class="method-section">
      <div class="home-container">
        <ScrollReveal class="section-heading">
          <div class="eyebrow">A quiet writing room</div>
          <h2>让创作回到<br /><span>自然流动</span>的状态</h2>
          <p>不必一次想完整，不必从空白开始。AI Passage Creator 把复杂的创作过程拆成四个可以被看见的小步骤。</p>
        </ScrollReveal>

        <StaggerList class="feature-grid">
          <article v-for="feature in features" :key="feature.index" class="feature-card">
            <div class="feature-topline"><span>{{ feature.index }}</span><component :is="feature.icon" /></div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <div class="feature-line" />
          </article>
        </StaggerList>
      </div>
    </section>

    <section class="metrics-section">
      <div class="home-container metrics-layout">
        <ScrollReveal class="metrics-copy">
          <div class="eyebrow">Your creative trace</div>
          <h2>每一次写下，<br />都会成为你的创作历程。</h2>
          <p>登录后，你的作品、创作进度与写作痕迹会被整理在个人空间中，随时可以回望，也可以继续向前。</p>
          <a-button type="link" class="text-link" @click="router.push('/profile')">查看个人空间 <ArrowRightOutlined /></a-button>
        </ScrollReveal>

        <StaggerList class="metrics-grid" :step="100">
          <article v-for="metric in metrics" :key="metric.label" class="metric-card">
            <div class="metric-value"><CountUpNumber :value="metric.value" /></div>
            <h3>{{ metric.label }}</h3>
            <p>{{ metric.note }}</p>
          </article>
        </StaggerList>
      </div>
    </section>

    <section v-if="loginUserStore.loginUser.id" class="recent-section">
      <div class="home-container">
        <ScrollReveal class="section-heading compact-heading">
          <div>
            <div class="eyebrow">Recent notes</div>
            <h2>最近留下的作品</h2>
            <p>从这里继续你的下一段表达。</p>
          </div>
          <a-button type="link" class="text-link" @click="goToList">查看全部 <ArrowRightOutlined /></a-button>
        </ScrollReveal>

        <a-spin :spinning="loadingArticles">
          <StaggerList v-if="recentArticles.length" class="article-grid">
            <article v-for="article in recentArticles" :key="article.id" class="article-card" @click="viewArticle(article)">
              <div class="article-cover">
                <img v-if="article.coverImage" :src="article.coverImage" :alt="article.mainTitle || article.topic" />
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
          </StaggerList>
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
  min-height: 650px;
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
  filter: blur(22px);
  animation: mist-drift 10s ease-in-out infinite alternate;
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

.hero-copy h1 {
  margin: 24px 0 18px;
  color: var(--ink-deep);
  font-size: clamp(3.1rem, 5.6vw, 5.4rem);
  font-weight: 600;
  line-height: 0.98;
}

.hero-copy h1 em {
  color: var(--mountain-green);
  font-style: normal;
}

.hero-subtitle {
  max-width: 600px;
  margin: 0 auto;
  color: var(--color-text-secondary);
  font-size: 17px;
  line-height: 1.8;
}

.creation-orbit {
  position: relative;
  width: 250px;
  height: 250px;
  position: absolute;
  top: 132px;
  right: 4%;
  opacity: 0.86;
  transform: scale(0.86);
  transform-origin: center;
}

.orbit-ring {
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(69, 111, 100, 0.24);
  border-radius: 50%;
  animation: orbit-spin 18s linear infinite;
}

.orbit-ring-inner { inset: 44px; border-style: dashed; animation-direction: reverse; animation-duration: 12s; }
.orbit-core {
  position: absolute;
  inset: 88px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: white;
  font-size: 28px;
  box-shadow: var(--shadow-green);
}

.orbit-label {
  position: absolute;
  padding: 6px 10px;
  border: 1px solid rgba(69, 111, 100, 0.16);
  border-radius: 999px;
  background: rgba(247, 250, 246, 0.78);
  color: var(--mountain-green);
  font-size: 12px;
}

.orbit-label-top { top: 0; left: 50%; transform: translateX(-50%); }
.orbit-label-right { right: -6px; top: 50%; transform: translateY(-50%); }
.orbit-label-bottom { bottom: 0; left: 50%; transform: translateX(-50%); }

.topic-composer {
  grid-column: 1;
  width: min(760px, calc(100% - 280px));
  justify-self: start;
  margin-top: 28px;
  padding: 18px;
  border: 1px solid rgba(69, 111, 100, 0.16);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(16px);
}

.composer-label { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: var(--ink-deep); font-size: 13px; font-weight: 600; }
.signal-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--mountain-green); box-shadow: 0 0 0 5px rgba(143, 184, 164, 0.24); }
.composer-row { display: flex; gap: 12px; }
.topic-input { flex: 1; border: 0 !important; box-shadow: none !important; background: rgba(255, 255, 255, 0.68) !important; }
.cta-button { display: inline-flex; align-items: center; gap: 8px; height: 48px; padding: 0 22px; white-space: nowrap; }
.composer-hint { margin: 10px 2px 0; color: var(--ink-muted); font-size: 12px; }

.method-section,
.recent-section { padding: 112px 0; background: var(--paper-warm); }
.section-heading { max-width: 650px; margin-bottom: 46px; }
.section-heading h2,
.metrics-copy h2 { margin: 18px 0 14px; color: var(--ink-deep); font-size: clamp(2.4rem, 5vw, 4.6rem); font-weight: 500; line-height: 1.04; }
.section-heading h2 span { color: var(--mountain-green); }
.section-heading p,
.metrics-copy p { max-width: 520px; margin: 0; color: var(--color-text-secondary); font-size: 16px; line-height: 1.85; }
.feature-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.feature-card { min-height: 245px; padding: 24px; border-top: 1px solid var(--line-soft); background: rgba(255,255,255,0.35); transition: transform var(--transition-normal), background var(--transition-normal), box-shadow var(--transition-normal); }
.feature-card:hover { transform: translateY(-8px); background: rgba(255,255,255,0.75); box-shadow: var(--shadow-card-hover); }
.feature-topline { display: flex; justify-content: space-between; color: var(--mountain-green); font-size: 18px; }
.feature-topline span { font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.feature-card h3 { margin: 42px 0 10px; color: var(--ink-deep); font-size: 20px; font-weight: 600; }
.feature-card p { min-height: 68px; margin: 0; color: var(--ink-muted); font-size: 14px; line-height: 1.7; }
.feature-line { width: 42px; height: 2px; margin-top: 28px; background: var(--river-green); }

.metrics-section { padding: 112px 0; background: linear-gradient(135deg, var(--ink-deep), #31574f); color: white; }
.metrics-layout { display: grid; grid-template-columns: 0.9fr 1.1fr; gap: 80px; align-items: center; }
.metrics-copy .eyebrow { color: var(--river-green); }
.metrics-copy h2 { color: white; }
.metrics-copy p { color: rgba(243,247,243,0.72); }
.text-link { padding: 0; color: var(--river-green); font-weight: 700; }
.text-link:hover { color: white; }
.metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.metric-card { padding: 24px 18px; border: 1px solid rgba(255,255,255,0.15); border-radius: var(--radius-lg); background: rgba(255,255,255,0.08); backdrop-filter: blur(12px); }
.metric-value { color: #d5eadb; font-family: 'Outfit', sans-serif; font-size: 44px; font-weight: 600; line-height: 1; }
.metric-card h3 { margin: 18px 0 6px; color: white; font-size: 15px; }
.metric-card p { margin: 0; color: rgba(243,247,243,0.62); font-size: 12px; }

.compact-heading { display: flex; align-items: end; justify-content: space-between; }
.compact-heading h2 { margin-bottom: 8px; font-size: clamp(2rem, 4vw, 3.4rem); }
.article-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.article-card { overflow: hidden; border: 1px solid var(--line-soft); border-radius: var(--radius-lg); background: rgba(255,255,255,0.72); cursor: pointer; transition: transform var(--transition-normal), box-shadow var(--transition-normal); }
.article-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-card-hover); }
.article-cover { display: grid; place-items: center; height: 160px; overflow: hidden; background: var(--color-background-tertiary); color: var(--mountain-green); font-size: 32px; }
.article-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition-slow); }
.article-card:hover .article-cover img { transform: scale(1.05); }
.article-info { padding: 18px; }
.article-info h3 { display: -webkit-box; overflow: hidden; margin: 0 0 16px; color: var(--ink-deep); font-size: 17px; font-weight: 600; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--ink-muted); font-size: 12px; }
.article-meta span:first-child { display: inline-flex; align-items: center; gap: 5px; }
.article-status { color: var(--mountain-green); }
.empty-articles { display: grid; justify-items: center; gap: 10px; padding: 58px 20px; border: 1px dashed var(--line-soft); border-radius: var(--radius-lg); color: var(--ink-muted); text-align: center; }
.empty-articles > :first-child { color: var(--river-green); font-size: 32px; }
.empty-articles strong { color: var(--ink-deep); }
.empty-articles span { margin-bottom: 8px; font-size: 13px; }

@keyframes mist-drift { from { transform: translate3d(-2%, 0, 0) scale(1); } to { transform: translate3d(4%, 10px, 0) scale(1.05); } }
@keyframes orbit-spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .hero-layout { gap: 24px; padding-top: 76px; }
  .creation-orbit { position: relative; top: auto; right: auto; order: 2; justify-self: center; width: 190px; height: 190px; transform: scale(0.9); }
  .orbit-core { inset: 66px; }
  .orbit-ring-inner { inset: 34px; }
  .orbit-ring-outer { inset: 4px; }
  .topic-composer { width: 100%; order: 3; justify-self: center; margin-top: 0; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .metrics-layout { grid-template-columns: 1fr; gap: 42px; }
}

@media (max-width: 640px) {
  .home-container { width: min(100% - 32px, 560px); }
  .home-hero { min-height: 700px; }
  .home-hero .hero-layout { padding: 76px 0 64px; }
  .hero-copy h1 { font-size: clamp(3rem, 15vw, 4.5rem); }
  .hero-subtitle { font-size: 15px; }
  .composer-row { flex-direction: column; }
  .cta-button { justify-content: center; }
  .method-section, .metrics-section, .recent-section { padding: 76px 0; }
  .section-heading h2, .metrics-copy h2 { font-size: 2.7rem; }
  .feature-grid, .metrics-grid, .article-grid { grid-template-columns: 1fr; }
  .feature-card { min-height: auto; }
  .feature-card h3 { margin-top: 28px; }
  .compact-heading { align-items: start; flex-direction: column; gap: 12px; }
}
</style>
