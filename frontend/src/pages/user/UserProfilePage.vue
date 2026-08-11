<template>
  <div class="profile-page">
    <div class="profile-orb orb-one"></div>
    <div class="profile-orb orb-two"></div>
    <div class="profile-stage" aria-hidden="true">
      <span
        v-for="section in sections"
        :key="`stage-${section.id}`"
        :class="['stage-layer', `stage-${section.id}`, { active: activeBackgroundSection === section.id }]"
        :style="{ backgroundImage: `url(${section.background})` }"
      ></span>
    </div>
    <div class="section-telemetry" aria-hidden="true">
      <strong>{{ String(activeSectionIndex).padStart(2, '0') }}</strong>
      <span>/ {{ String(sections.length).padStart(2, '0') }}</span>
    </div>

    <div class="profile-layout">
      <aside class="section-menu" aria-label="个人资料分段导航">
        <p class="menu-caption">PROFILE / 个人空间</p>
        <button
          v-for="section in sections"
          :key="section.id"
          type="button"
          :class="['menu-item', { active: activeSection === section.id }]"
          @click="scrollToSection(section.id)"
        >
          <span class="menu-index">{{ section.index }}</span>
          <span>{{ section.label }}</span>
        </button>
      </aside>

      <main class="profile-content">
        <section
          id="profile-overview"
          class="profile-section hero-section"
          :class="{ revealed: isVisible('profile-overview'), 'is-loading': isSectionLoading('profile-overview') }"
        >
          <div v-if="isSectionLoading('profile-overview')" class="section-loading" aria-live="polite">
            <span class="loading-orbit"></span>
            <span>正在整理你的个人空间</span>
          </div>
          <div class="section-content" :class="{ 'content-hidden': !isVisible('profile-overview') }">
            <div class="eyebrow">个人资料 · PERSONAL SPACE</div>
            <div class="hero-grid">
            <div class="hero-copy">
              <p class="hero-kicker">记录每一次灵感落笔</p>
              <h1>{{ loginUserStore.loginUser.userName || '无名创作者' }}</h1>
              <p class="hero-description">
                {{ loginUserStore.loginUser.userProfile || '在这里整理你的创作轨迹，让每一篇文章都留下清晰的时间印记。' }}
              </p>
              <p class="hero-supporting-copy">
                从灵感整理到正文落笔，持续完善你的创作者名片；每一次创作都会留下清晰的时间线、数据与作品记录。
              </p>
              <div class="hero-actions">
                <button type="button" class="primary-action" @click="scrollToSection('edit-profile')">
                  编辑资料
                  <ArrowRightOutlined />
                </button>
                <button type="button" class="ghost-action" @click="scrollToSection('my-works')">
                  查看我的作品
                </button>
              </div>
              <div class="hero-insight-grid">
                <article class="hero-insight-card">
                  <span>创作档案</span>
                  <strong>{{ stats.totalWorks || '—' }}</strong>
                  <small>{{ stats.totalWorks ? '篇作品已记录' : '进入创作数据查看' }}</small>
                </article>
                <article class="hero-insight-card">
                  <span>平均篇幅</span>
                  <strong>{{ averageCharacters ? averageCharacters.toLocaleString() : '—' }}</strong>
                  <small>{{ averageCharacters ? '字 / 篇' : '等待数据同步' }}</small>
                </article>
                <article class="hero-insight-card">
                  <span>创作状态</span>
                  <strong>{{ stats.totalWorks ? '持续中' : '待开始' }}</strong>
                  <small>{{ stats.latestWorkTime ? `最近记录 ${formatDate(stats.latestWorkTime)}` : '准备好写下第一篇' }}</small>
                </article>
              </div>
            </div>

            <div class="avatar-panel">
              <input
                ref="avatarInput"
                class="avatar-input"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                @change="handleAvatarChange"
              />
              <button type="button" class="avatar-button" :disabled="uploadingAvatar" @click="openAvatarPicker">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="148" class="profile-avatar">
                  {{ (loginUserStore.loginUser.userName || '无').slice(0, 1) }}
                </a-avatar>
                <span class="avatar-overlay">
                  <CameraOutlined />
                  {{ uploadingAvatar ? '上传中…' : '更换头像' }}
                </span>
              </button>
              <span class="avatar-tip">PNG / JPG / WebP · 最大 5MB</span>
            </div>
            </div>
          </div>
        </section>

        <section
          id="creation-stats"
          class="profile-section stats-section"
          :class="{ revealed: isVisible('creation-stats'), 'is-loading': isSectionLoading('creation-stats') }"
        >
          <div v-if="isSectionLoading('creation-stats')" class="section-loading" aria-live="polite">
            <span class="loading-orbit"></span>
            <span>正在读取创作数据</span>
          </div>
          <div class="section-content" :class="{ 'content-hidden': !isVisible('creation-stats') }">
            <div class="section-heading">
            <div>
              <div class="eyebrow">CREATION DATA</div>
              <h2>把创作变成可以看见的轨迹</h2>
            </div>
            <span class="heading-note">数据会在进入视口后开始加载</span>
            </div>
            <div class="stats-grid">
            <article v-for="stat in displayStats" :key="stat.key" class="stat-card">
              <span class="stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <span class="stat-unit">{{ stat.unit }}</span>
              <span class="stat-line"></span>
            </article>
            </div>
            <div class="stats-detail-grid">
              <article class="stats-chart-card">
                <div class="stats-panel-heading">
                  <div>
                    <span class="panel-kicker">CREATION PULSE</span>
                    <h3>创作完成度</h3>
                  </div>
                  <span class="panel-caption">根据当前作品状态实时计算</span>
                </div>
                <div class="stats-chart-body">
                  <div
                    class="completion-ring"
                    :style="{ background: `conic-gradient(#1d9e52 ${completionRate}%, rgba(29, 158, 82, .12) ${completionRate}% 100%)` }"
                  >
                    <div class="completion-ring-inner">
                      <strong>{{ completionRate }}%</strong>
                      <span>完成率</span>
                    </div>
                  </div>
                  <div class="status-bars">
                    <div v-for="metric in workStatusMetrics" :key="metric.label" class="status-bar-row">
                      <div class="status-bar-label">
                        <span>{{ metric.label }}</span>
                        <strong>{{ metric.value }} 篇</strong>
                      </div>
                      <div class="status-bar-track">
                        <span :style="{ width: `${metric.percent}%` }"></span>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
              <article class="stats-table-card">
                <div class="stats-panel-heading">
                  <div>
                    <span class="panel-kicker">DATA SNAPSHOT</span>
                    <h3>创作数据明细</h3>
                  </div>
                </div>
                <table class="stats-table">
                  <tbody>
                    <tr v-for="row in statsRows" :key="row.label">
                      <th>{{ row.label }}</th>
                      <td>{{ row.value }}<small>{{ row.unit }}</small></td>
                      <td class="stats-row-note">{{ row.note }}</td>
                    </tr>
                  </tbody>
                </table>
              </article>
            </div>
          </div>
        </section>

        <section
          id="creation-history"
          class="profile-section history-section"
          :class="{ revealed: isVisible('creation-history'), 'is-loading': isSectionLoading('creation-history') }"
        >
          <div v-if="isSectionLoading('creation-history')" class="section-loading" aria-live="polite">
            <span class="loading-orbit"></span>
            <span>正在展开创作历程</span>
          </div>
          <div class="section-content" :class="{ 'content-hidden': !isVisible('creation-history') }">
            <div class="section-heading">
            <div>
              <div class="eyebrow">CREATION JOURNEY</div>
              <h2>你的创作历程</h2>
            </div>
            </div>
            <div class="journey-card">
            <div class="journey-point first-point">
              <span class="point-dot"></span>
              <div>
                <span class="point-time">{{ formatDate(loginUserStore.loginUser.createTime) }}</span>
                <h3>加入 AI 文章创作空间</h3>
                <p>从第一次登录开始，创作记录就会被认真保存。</p>
              </div>
            </div>
            <div class="journey-line"></div>
            <div class="journey-point">
              <span class="point-dot current"></span>
              <div>
                <span class="point-time">{{ formatDate(stats.latestWorkTime) }}</span>
                <h3>{{ stats.totalWorks ? '持续创作中' : '等待第一篇作品' }}</h3>
                <p>{{ stats.totalWorks ? `已经留下 ${stats.totalWorks} 篇作品，继续写下去。` : '写下第一个主题，让这条时间线开始生长。' }}</p>
              </div>
            </div>
            </div>
          </div>
        </section>

        <section
          id="my-works"
          class="profile-section works-section"
          :class="{ revealed: isVisible('my-works'), 'is-loading': isSectionLoading('my-works') }"
        >
          <div v-if="isSectionLoading('my-works')" class="section-loading" aria-live="polite">
            <span class="loading-orbit"></span>
            <span>正在重新加载作品正文</span>
          </div>
          <div class="section-content" :class="{ 'content-hidden': !isVisible('my-works') }">
            <div class="section-heading works-heading">
            <div>
              <div class="eyebrow">MY WORKS</div>
              <h2>作品正文记录</h2>
              <p>完整保留每一次生成的正文，进入这一段后才会按页加载。</p>
            </div>
            <button type="button" class="outline-action" @click="router.push('/article/list')">
              管理全部作品 <ArrowRightOutlined />
            </button>
            </div>

            <div v-if="worksError" class="state-card error-state">
            <WarningOutlined />
            <span>{{ worksError }}</span>
            <button type="button" @click="loadWorks(true)">重新加载</button>
            </div>
            <div v-else-if="!worksLoaded && worksLoading" class="works-loading">
            <div v-for="item in 2" :key="item" class="article-skeleton">
              <span></span><i></i><em></em><em></em>
            </div>
            </div>
            <div v-else-if="worksLoaded && !articles.length" class="state-card empty-state">
            <FileTextOutlined />
            <h3>还没有作品记录</h3>
            <p>去创作页写下第一个主题，这里会完整保存你的正文。</p>
            <button type="button" @click="router.push('/create')">开始创作</button>
            </div>
            <div v-else class="article-stream">
            <article v-for="(article, index) in articles" :key="getArticleKey(article)" class="article-record">
              <div class="article-index">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="article-meta">
                <span>{{ formatDate(article.createTime) }}</span>
                <span class="article-status">{{ getStatusText(article.status) }}</span>
              </div>
              <button
                type="button"
                class="article-title"
                :aria-expanded="isArticleExpanded(article)"
                @click="toggleArticleExpanded(article)"
              >
                {{ article.mainTitle || article.topic || '未命名作品' }}
                <ArrowUpOutlined :class="{ 'article-title-icon-collapsed': !isArticleExpanded(article) }" />
              </button>
              <p v-if="article.subTitle" class="article-subtitle">{{ article.subTitle }}</p>
              <div v-if="isArticleExpanded(article)" class="article-body" v-html="renderArticle(article)"></div>
            </article>
            <div ref="worksSentinel" class="works-sentinel">
              <span v-if="worksLoading">正在加载下一段作品…</span>
              <span v-else-if="hasMoreWorks">向下滚动加载更多</span>
              <span v-else>已经看到全部作品</span>
            </div>
            </div>
          </div>
        </section>

        <section
          id="edit-profile"
          class="profile-section edit-section"
          :class="{ revealed: isVisible('edit-profile'), 'is-loading': isSectionLoading('edit-profile') }"
        >
          <div v-if="isSectionLoading('edit-profile')" class="section-loading" aria-live="polite">
            <span class="loading-orbit"></span>
            <span>正在打开资料工作台</span>
          </div>
          <div class="section-content" :class="{ 'content-hidden': !isVisible('edit-profile') }">
            <div class="section-heading">
            <div>
              <div class="eyebrow">EDIT PROFILE</div>
              <h2>完善你的创作者名片</h2>
              <p>QQ 邮箱、电话、博客和 GitHub 地址只对你的个人资料页可见。</p>
            </div>
            </div>
            <a-card :bordered="false" class="edit-card">
            <a-form :model="profileForm" layout="vertical" @finish="saveProfile">
              <div class="form-grid">
                <a-form-item label="昵称" name="userName">
                  <a-input v-model:value="profileForm.userName" placeholder="输入你的创作者昵称" />
                </a-form-item>
                <a-form-item label="QQ 邮箱" name="userEmail">
                  <a-input v-model:value="profileForm.userEmail" placeholder="例如 poet@qq.com" />
                </a-form-item>
                <a-form-item label="电话号码" name="userPhone">
                  <a-input v-model:value="profileForm.userPhone" placeholder="输入联系电话" />
                </a-form-item>
                <a-form-item label="个人博客" name="userBlog">
                  <a-input v-model:value="profileForm.userBlog" placeholder="https://example.com" />
                </a-form-item>
                <a-form-item label="GitHub 地址" name="userGithub">
                  <a-input v-model:value="profileForm.userGithub" placeholder="https://github.com/your-name" />
                </a-form-item>
                <a-form-item label="个人简介" name="userProfile" class="full-field">
                  <a-textarea v-model:value="profileForm.userProfile" :rows="4" placeholder="用一句话介绍你的创作方向" />
                </a-form-item>
              </div>
              <div class="form-footer">
                <span>修改后会同步到顶部个人资料和登录态。</span>
                <a-button type="primary" html-type="submit" :loading="savingProfile" class="save-button">保存资料</a-button>
              </div>
            </a-form>
            </a-card>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  ArrowUpOutlined,
  CameraOutlined,
  FileTextOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { marked } from 'marked'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { getUserArticleStats, listArticle } from '@/api/articleController.ts'
import { updateMyProfile, uploadAvatar } from '@/api/userController.ts'
import profileOverviewBackground from '@/assets/profile/profile-overview-bg.png'
import creationStatsBackground from '@/assets/profile/creation-stats-bg.png'
import creationHistoryBackground from '@/assets/profile/creation-history-bg.png'
import myWorksBackground from '@/assets/profile/my-works-bg.png'
import editProfileBackground from '@/assets/profile/edit-profile-bg.png'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const sections = [
  { id: 'profile-overview', index: '01', label: '个人概览', background: profileOverviewBackground },
  { id: 'creation-stats', index: '02', label: '创作数据', background: creationStatsBackground },
  { id: 'creation-history', index: '03', label: '创作历程', background: creationHistoryBackground },
  { id: 'my-works', index: '04', label: '我的作品', background: myWorksBackground },
  { id: 'edit-profile', index: '05', label: '编辑资料', background: editProfileBackground },
]

const activeSection = ref('profile-overview')
const activeBackgroundSection = ref('profile-overview')
const activeSectionIndex = computed(() => Math.max(1, sections.findIndex((section) => section.id === activeSection.value) + 1))
const pendingScrollSection = ref<string | null>(null)
const visibleSections = ref(new Set<string>())
const sectionLoading = ref(new Set<string>())
const sectionVisit = reactive<Record<string, number>>({})
const sectionInView = new Set<string>()
const sectionTimers = new Map<string, number>()
const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
let sectionObserver: IntersectionObserver | null = null
let syncActiveSection: (() => void) | null = null
let navigationTimer: number | null = null

const stats = reactive({
  totalWorks: 0,
  completedWorks: 0,
  totalCharacters: 0,
  latestWorkTime: '',
})
const animatedStats = reactive({ totalWorks: 0, completedWorks: 0, totalCharacters: 0 })
const statsLoaded = ref(false)
let statsPromise: Promise<void> | null = null

const displayStats = computed(() => [
  { key: 'totalWorks', label: '累计作品', value: animatedStats.totalWorks, unit: '篇' },
  { key: 'completedWorks', label: '完成创作', value: animatedStats.completedWorks, unit: '篇' },
  { key: 'totalCharacters', label: '正文字符', value: animatedStats.totalCharacters.toLocaleString(), unit: '字' },
])

const pendingWorks = computed(() => Math.max(stats.totalWorks - stats.completedWorks, 0))
const completionRate = computed(() => stats.totalWorks > 0
  ? Math.min(100, Math.round((stats.completedWorks / stats.totalWorks) * 100))
  : 0)
const averageCharacters = computed(() => stats.totalWorks > 0
  ? Math.round(stats.totalCharacters / stats.totalWorks)
  : 0)
const workStatusMetrics = computed(() => {
  const total = Math.max(stats.totalWorks, 1)
  return [
    { label: '已完成', value: stats.completedWorks, percent: Math.min(100, Math.round((stats.completedWorks / total) * 100)) },
    { label: '待继续', value: pendingWorks.value, percent: Math.min(100, Math.round((pendingWorks.value / total) * 100)) },
  ]
})
const statsRows = computed(() => [
  { label: '累计作品', value: stats.totalWorks.toLocaleString(), unit: '篇', note: '全部创作记录' },
  { label: '完成创作', value: stats.completedWorks.toLocaleString(), unit: '篇', note: `${completionRate.value}% 完成率` },
  { label: '正文字符', value: stats.totalCharacters.toLocaleString(), unit: '字', note: '所有正文累计' },
  { label: '平均篇幅', value: averageCharacters.value.toLocaleString(), unit: '字/篇', note: '按累计作品计算' },
])

const profileForm = reactive<API.UserProfileUpdateRequest>({})
const savingProfile = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const uploadingAvatar = ref(false)

const articles = ref<API.ArticleVO[]>([])
const expandedArticleKeys = ref(new Set<string>())
const worksPage = ref(1)
const worksTotalPage = ref(1)
const worksLoaded = ref(false)
const worksLoading = ref(false)
const worksError = ref('')
const worksSentinel = ref<HTMLElement | null>(null)
let worksObserver: IntersectionObserver | null = null

const hasMoreWorks = computed(() => worksPage.value < worksTotalPage.value)

const isVisible = (id: string) => visibleSections.value.has(id)
const isSectionLoading = (id: string) => sectionLoading.value.has(id)

const updateSet = (target: typeof visibleSections | typeof sectionLoading, id: string, shouldInclude: boolean) => {
  const next = new Set(target.value)
  if (shouldInclude) next.add(id)
  else next.delete(id)
  target.value = next
}

const syncProfileForm = () => {
  const user = loginUserStore.loginUser
  Object.assign(profileForm, {
    userName: user.userName || '',
    userProfile: user.userProfile || '',
    userEmail: user.userEmail || '',
    userPhone: user.userPhone || '',
    userBlog: user.userBlog || '',
    userGithub: user.userGithub || '',
  })
}

watch(() => loginUserStore.loginUser, syncProfileForm, { deep: true, immediate: true })

const scrollToSection = (id: string) => {
  pendingScrollSection.value = id
  activeSection.value = id
  void beginSectionVisit(id)
  document.getElementById(id)?.scrollIntoView({ behavior: prefersReducedMotion ? 'auto' : 'smooth', block: 'start' })
  if (navigationTimer) window.clearTimeout(navigationTimer)
  const deadline = Date.now() + (prefersReducedMotion ? 300 : 3200)
  const settleNavigation = () => {
    if (pendingScrollSection.value !== id) return
    const target = document.getElementById(id)
    const targetTop = target?.getBoundingClientRect().top
    const isAligned = targetTop !== undefined && Math.abs(targetTop - 88) <= 32
    if (isAligned) {
      pendingScrollSection.value = null
      navigationTimer = null
      syncActiveSection?.()
      return
    }
    if (Date.now() >= deadline) {
      target?.scrollIntoView({ behavior: 'auto', block: 'start' })
      pendingScrollSection.value = null
      navigationTimer = null
      syncActiveSection?.()
      return
    }
    navigationTimer = window.setTimeout(settleNavigation, 50)
  }
  navigationTimer = window.setTimeout(settleNavigation, 50)
}

const animateNumber = (key: 'totalWorks' | 'completedWorks' | 'totalCharacters', target: number) => {
  if (prefersReducedMotion || target <= 0) {
    animatedStats[key] = target
    return
  }
  const start = performance.now()
  const duration = 1000
  const tick = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedStats[key] = Math.floor(target * eased)
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

const loadStats = async () => {
  if (statsLoaded.value) return
  if (statsPromise) return statsPromise

  statsPromise = (async () => {
    try {
      const res = await getUserArticleStats()
      const data = res.data.data
      if (res.data.code !== 0 || !data) throw new Error(res.data.message || '创作统计加载失败')
      stats.totalWorks = data.totalWorks || 0
      stats.completedWorks = data.completedWorks || 0
      stats.totalCharacters = data.totalCharacters || 0
      stats.latestWorkTime = data.latestWorkTime || ''
    } catch (error) {
      console.error(error)
    } finally {
      statsLoaded.value = true
    }
  })()

  try {
    await statsPromise
  } finally {
    statsPromise = null
  }
}

const waitForReveal = (duration: number) => new Promise<void>((resolve) => window.setTimeout(resolve, duration))

const resetSectionState = (id: string, invalidate = true) => {
  if (invalidate) sectionVisit[id] = (sectionVisit[id] || 0) + 1
  const timer = sectionTimers.get(id)
  if (timer) window.clearTimeout(timer)
  sectionTimers.delete(id)
  updateSet(sectionLoading, id, false)
  updateSet(visibleSections, id, false)
}

const beginSectionVisit = async (id: string) => {
  const visit = (sectionVisit[id] || 0) + 1
  sectionVisit[id] = visit
  activeBackgroundSection.value = id

  if (isVisible(id) && !isSectionLoading(id)) return

  resetSectionState(id, false)
  updateSet(visibleSections, id, true)
  updateSet(sectionLoading, id, true)

  if (id === 'creation-stats') {
    statsLoaded.value = false
    animatedStats.totalWorks = 0
    animatedStats.completedWorks = 0
    animatedStats.totalCharacters = 0
  }

  const loadTask = (id === 'profile-overview' || id === 'creation-stats') && !statsLoaded.value
    ? loadStats()
    : id === 'my-works'
      ? loadWorks(true)
      : Promise.resolve()

  void loadTask.then(() => {
    if (sectionVisit[id] !== visit || activeSection.value !== id) return
    if (id === 'creation-stats') {
      animateNumber('totalWorks', stats.totalWorks)
      animateNumber('completedWorks', stats.completedWorks)
      animateNumber('totalCharacters', stats.totalCharacters)
    }
  })

  await nextTick()
  await waitForReveal(prefersReducedMotion ? 0 : 80)

  if (sectionVisit[id] !== visit || activeSection.value !== id) return
  updateSet(sectionLoading, id, false)
}

const loadWorks = async (reset = false) => {
  if (worksLoading.value || (!reset && worksLoaded.value && !hasMoreWorks.value)) return
  if (reset) {
    worksPage.value = 1
    worksTotalPage.value = 1
    articles.value = []
    worksLoaded.value = false
    worksError.value = ''
  }
  worksLoading.value = true
  try {
    const res = await listArticle({ pageNum: worksPage.value, pageSize: 3 })
    const page = res.data.data
    if (res.data.code !== 0 || !page) throw new Error(res.data.message || '作品加载失败')
    articles.value = [...articles.value, ...(page.records || [])]
    worksTotalPage.value = page.totalPage || 1
    worksLoaded.value = true
    worksPage.value += 1
    await nextTick()
    if (worksSentinel.value && worksObserver) worksObserver.observe(worksSentinel.value)
  } catch (error) {
    worksError.value = error instanceof Error ? error.message : '作品加载失败'
  } finally {
    worksLoading.value = false
  }
}

const renderArticle = (article: API.ArticleVO) => {
  const source = article.fullContent || article.content || '这篇作品暂时没有正文内容。'
  return marked.parse(source) as string
}

const getArticleKey = (article: API.ArticleVO) => String(article.taskId || article.id || '')

const isArticleExpanded = (article: API.ArticleVO) => expandedArticleKeys.value.has(getArticleKey(article))

const toggleArticleExpanded = (article: API.ArticleVO) => {
  const key = getArticleKey(article)
  const next = new Set(expandedArticleKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedArticleKeys.value = next
}

const formatDate = (value?: string) => (value ? dayjs(value).format('YYYY.MM.DD HH:mm') : '尚未开始')

const getStatusText = (status?: string) => {
  const labels: Record<string, string> = { COMPLETED: '已完成', PROCESSING: '生成中', PENDING: '等待中', FAILED: '失败' }
  return labels[status || ''] || '创作记录'
}

const openAvatarPicker = () => avatarInput.value?.click()

const handleAvatarChange = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    message.error('头像只支持 PNG、JPG 或 WebP')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('头像不能超过 5MB')
    return
  }
  uploadingAvatar.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '头像上传失败')
    loginUserStore.setLoginUser(res.data.data)
    message.success('头像更新成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '头像上传失败')
  } finally {
    uploadingAvatar.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

const saveProfile = async () => {
  savingProfile.value = true
  try {
    const res = await updateMyProfile({ ...profileForm })
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '资料保存失败')
    loginUserStore.setLoginUser(res.data.data)
    message.success('资料保存成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '资料保存失败')
  } finally {
    savingProfile.value = false
  }
}

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
  await nextTick()
  const sectionElements = Array.from(document.querySelectorAll<HTMLElement>('.profile-section'))
  syncActiveSection = () => {
    const candidate = sectionElements
      .map((element) => {
        const rect = element.getBoundingClientRect()
        return { element, top: rect.top, bottom: rect.bottom }
      })
      .filter(({ top, bottom }) => top < window.innerHeight * 0.62 && bottom > window.innerHeight * 0.08)
      .sort((a, b) => Math.abs(a.top - 112) - Math.abs(b.top - 112))[0]

    if (!candidate) return

    let id = candidate.element.id
    const pendingId = pendingScrollSection.value
    if (pendingId) {
      const pendingElement = document.getElementById(pendingId)
      const pendingTop = pendingElement?.getBoundingClientRect().top
      if (pendingTop !== undefined && Math.abs(pendingTop - 88) > 32) {
        id = pendingId
      } else {
        pendingScrollSection.value = null
      }
    }
    const changed = activeSection.value !== id
    activeSection.value = id
    if (changed || (!isVisible(id) && !isSectionLoading(id))) {
      void beginSectionVisit(id)
    }
  }
  sectionObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        const id = (entry.target as HTMLElement).id
        if (entry.isIntersecting) {
          sectionInView.add(id)
          return
        }
        sectionInView.delete(id)
      })
      syncActiveSection?.()
    },
    { rootMargin: '-12% 0px -45% 0px', threshold: 0.05 },
  )
  sectionElements.forEach((element) => sectionObserver?.observe(element))
  window.addEventListener('scroll', syncActiveSection, { passive: true })
  syncActiveSection()

  worksObserver = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadWorks()
    },
    { rootMargin: '280px 0px' },
  )
  if (worksSentinel.value) worksObserver.observe(worksSentinel.value)
})

onBeforeUnmount(() => {
  sectionObserver?.disconnect()
  worksObserver?.disconnect()
  if (syncActiveSection) window.removeEventListener('scroll', syncActiveSection)
  if (navigationTimer) window.clearTimeout(navigationTimer)
  sectionTimers.forEach((timer) => window.clearTimeout(timer))
})
</script>

<style scoped lang="scss">
.profile-page {
  --profile-ink: #16352a;
  --profile-muted: #71877a;
  --profile-line: rgba(22, 53, 42, 0.11);
  --profile-scene-height: max(620px, calc(100svh - 136px));
  position: relative;
  min-height: calc(100vh - 64px);
  overflow-x: clip;
  overflow-y: visible;
  background: linear-gradient(180deg, #f9fcfa 0%, #f2f8f3 52%, #fbfdfb 100%);
  color: var(--profile-ink);
}

.profile-stage { position: fixed; inset: 0; z-index: 0; overflow: hidden; pointer-events: none; }
.stage-layer { position: absolute; inset: 0; opacity: 0; background-position: center; background-size: cover; background-repeat: no-repeat; transition: opacity .45s ease; will-change: opacity; }
.stage-layer.active { opacity: 1; }
.stage-layer::after { position: absolute; inset: 0; content: ''; background: rgba(245, 252, 247, .18); }
.stage-profile-overview::after { background: linear-gradient(120deg, rgba(247, 253, 249, .48), rgba(219, 244, 226, .12)); }
.stage-creation-stats::after { background: linear-gradient(120deg, rgba(216, 246, 241, .4), rgba(231, 249, 242, .12)); }
.stage-creation-history::after { background: linear-gradient(120deg, rgba(255, 252, 241, .42), rgba(242, 248, 234, .12)); }
.stage-my-works::after { background: linear-gradient(120deg, rgba(222, 239, 225, .3), rgba(245, 252, 246, .1)); }
.stage-edit-profile::after { background: linear-gradient(120deg, rgba(255, 255, 252, .52), rgba(222, 244, 230, .1)); }

.section-telemetry {
  position: fixed;
  right: 28px;
  bottom: 26px;
  z-index: 20;
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  color: rgba(22, 53, 42, .6);
  font-size: 12px;
  letter-spacing: .08em;
  pointer-events: none;
}

.section-telemetry strong {
  color: #1d9e52;
  font-size: 30px;
  line-height: 1;
  letter-spacing: -.08em;
  transition: color .25s ease, transform .25s ease;
}

.section-telemetry span { color: rgba(22, 53, 42, .42); }

.profile-orb {
  position: absolute;
  width: 440px;
  height: 440px;
  border-radius: 50%;
  filter: blur(6px);
  pointer-events: none;
  opacity: .42;
}

.orb-one { top: -180px; right: -160px; background: radial-gradient(circle, rgba(100, 213, 146, .4), transparent 68%); }
.orb-two { top: 800px; left: -240px; background: radial-gradient(circle, rgba(188, 227, 199, .55), transparent 68%); }

.profile-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 184px minmax(0, 1fr);
  gap: 48px;
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 24px 100px;
}

.section-menu {
  position: sticky;
  top: 96px;
  align-self: start;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  width: 184px;
  margin-bottom: 0;
  padding: 14px 10px;
  overflow: hidden;
  border: 1px solid rgba(87, 155, 109, .14);
  border-radius: 22px;
  background: rgba(249, 252, 250, .84);
  box-shadow: 0 12px 30px rgba(56, 111, 73, .06);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.menu-caption, .eyebrow { margin: 0; color: #5a9b73; font-size: 11px; font-weight: 700; letter-spacing: .18em; }
.menu-caption { display: block; padding: 4px 10px 12px; line-height: 1.5; }
.menu-item {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 12px 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--profile-muted);
  font-size: 14px;
  white-space: nowrap;
  text-align: left;
  cursor: pointer;
  transition: color .25s ease, background .25s ease, box-shadow .25s ease;
}
.menu-item:hover { color: var(--profile-ink); background: rgba(223, 243, 229, .62); }
.menu-item.active { color: #1d8f4d; background: #dff3e5; box-shadow: 0 8px 18px rgba(53, 155, 83, .1); }
.menu-index { width: auto; color: #82ad91; font-size: 12px; }
.menu-item.active .menu-index { color: #1eac59; }

.profile-content { min-width: 0; }
.profile-section {
  position: relative;
  min-height: var(--profile-scene-height);
  padding: 64px 0;
  border-bottom: 1px solid var(--profile-line);
  opacity: 0;
  transform: translateY(22px);
  transition: opacity .48s ease, transform .48s ease;
  scroll-margin-top: 88px;
  isolation: isolate;
  overflow: hidden;
}
.profile-section::before {
  position: absolute;
  inset: 8% -8%;
  z-index: -1;
  border-radius: 42%;
  opacity: .34;
  content: '';
  pointer-events: none;
  transition: opacity .45s ease;
}
.profile-section.revealed::before { opacity: .52; }
.profile-section::after { position: absolute; inset: 0; z-index: -1; content: ''; pointer-events: none; opacity: .24; transition: opacity .45s ease; }
.hero-section::before { background: radial-gradient(circle at 78% 30%, rgba(145, 223, 166, .34), transparent 38%), radial-gradient(circle at 12% 78%, rgba(244, 253, 247, .58), transparent 34%); }
.hero-section::after { background: radial-gradient(circle at 76% 28%, rgba(255, 255, 255, .44) 0 1px, transparent 2px), radial-gradient(circle at 18% 68%, rgba(255, 255, 255, .28) 0 1px, transparent 2px); background-size: 150px 150px, 220px 220px; }
.stats-section::before { background: radial-gradient(circle at 20% 42%, rgba(104, 213, 190, .3), transparent 38%), radial-gradient(circle at 86% 66%, rgba(123, 211, 190, .22), transparent 32%); }
.stats-section::after { background: radial-gradient(circle at 76% 26%, rgba(255, 255, 255, .34) 0 1px, transparent 2px), linear-gradient(115deg, transparent 42%, rgba(107, 210, 181, .12) 43%, transparent 44%); background-size: 170px 170px, 100% 100%; }
.history-section::before { background: radial-gradient(circle at 82% 55%, rgba(207, 188, 124, .22), transparent 34%), radial-gradient(circle at 14% 36%, rgba(255, 248, 218, .42), transparent 32%); }
.history-section::after { background: radial-gradient(circle at 72% 32%, rgba(255, 247, 206, .34) 0 1px, transparent 2px), radial-gradient(circle at 24% 76%, rgba(255, 255, 255, .28) 0 1px, transparent 2px); background-size: 190px 190px, 240px 240px; }
.works-section::before { background: radial-gradient(circle at 18% 48%, rgba(62, 147, 104, .26), transparent 36%), radial-gradient(circle at 88% 22%, rgba(170, 231, 185, .24), transparent 30%); }
.works-section::after { background: radial-gradient(circle at 20% 30%, rgba(255, 255, 255, .34) 0 1px, transparent 2px), linear-gradient(145deg, transparent 48%, rgba(105, 194, 128, .11) 49%, transparent 50%); background-size: 160px 160px, 100% 100%; }
.edit-section::before { background: radial-gradient(circle at 76% 40%, rgba(175, 222, 188, .3), transparent 36%), radial-gradient(circle at 22% 76%, rgba(255, 255, 255, .46), transparent 32%); }
.edit-section::after { background: radial-gradient(circle at 78% 32%, rgba(255, 255, 255, .38) 0 1px, transparent 2px), radial-gradient(circle at 28% 68%, rgba(220, 255, 229, .3) 0 1px, transparent 2px); background-size: 180px 180px, 230px 230px; }
.profile-section > :not(.section-loading) { position: relative; z-index: 1; transition: opacity .35s ease, transform .35s ease; }
.profile-section.is-loading > :not(.section-loading) { opacity: 1; transform: none; pointer-events: auto; }
.section-content { position: relative; z-index: 1; transition: opacity .3s ease, transform .42s ease; }
.section-content.content-hidden { visibility: hidden; opacity: 0; transform: translateY(14px); pointer-events: none; }
.section-loading { position: absolute; top: 24px; right: 0; z-index: 3; display: flex; flex-direction: row; align-items: center; justify-content: center; gap: 8px; min-height: 32px; padding: 7px 12px; border: 1px solid rgba(255, 255, 255, .76); border-radius: 999px; color: #527563; font-size: 12px; letter-spacing: .04em; white-space: nowrap; background: rgba(249, 253, 250, .76); box-shadow: 0 10px 24px rgba(31, 92, 58, .08); backdrop-filter: blur(14px) saturate(115%); -webkit-backdrop-filter: blur(14px) saturate(115%); pointer-events: none; }
.loading-orbit { width: 18px; height: 18px; border: 2px solid rgba(67, 167, 100, .18); border-top-color: #2ba35a; border-right-color: #8bd09e; border-radius: 50%; animation: loading-orbit 1s linear infinite; }
@keyframes loading-orbit { to { transform: rotate(360deg); } }
.profile-section.revealed { opacity: 1; transform: translateY(0); }
.hero-section {
  box-sizing: border-box;
  min-height: 100svh;
  padding-top: 64px;
}
.stats-section { min-height: var(--profile-scene-height); }
.history-section { min-height: var(--profile-scene-height); }
.works-section { min-height: var(--profile-scene-height); }
.edit-section { min-height: var(--profile-scene-height); }
.hero-grid { display: grid; grid-template-columns: 1fr 260px; align-items: center; gap: 64px; min-height: 400px; }
.hero-kicker { margin: 32px 0 12px; color: #5c876d; font-size: 15px; letter-spacing: .08em; }
h1, h2, h3, p { margin-top: 0; }
h1 { max-width: 650px; margin-bottom: 20px; font-size: clamp(48px, 7vw, 86px); line-height: .98; letter-spacing: -.07em; font-weight: 700; }
h2 { margin: 12px 0 10px; font-size: clamp(30px, 4vw, 50px); line-height: 1.05; letter-spacing: -.055em; }
.hero-description { max-width: 560px; color: var(--profile-muted); font-size: 17px; line-height: 1.9; }
.hero-supporting-copy { max-width: 580px; margin: 14px 0 0; color: #789183; font-size: 14px; line-height: 1.8; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 34px; }
.hero-insight-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; max-width: 610px; margin-top: 28px; }
.hero-insight-card { min-width: 0; padding: 14px 15px; border: 1px solid rgba(255, 255, 255, .7); border-radius: 16px; background: rgba(255, 255, 255, .52); box-shadow: 0 14px 34px rgba(31, 92, 58, .06), inset 0 1px 0 rgba(255, 255, 255, .6); backdrop-filter: blur(14px) saturate(110%); -webkit-backdrop-filter: blur(14px) saturate(110%); }
.hero-insight-card span { display: block; color: #6b927a; font-size: 11px; }
.hero-insight-card strong { display: block; margin-top: 8px; color: #1d8f4d; font-size: 22px; line-height: 1; letter-spacing: -.04em; }
.hero-insight-card small { display: block; margin-top: 7px; overflow: hidden; color: #8aa496; font-size: 10px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.primary-action, .ghost-action, .outline-action, .state-card button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 48px; border-radius: 999px; padding: 0 22px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .25s ease; }
.primary-action { border: 0; color: #fff; background: #1d9e52; box-shadow: 0 12px 28px rgba(29, 158, 82, .22); }
.primary-action:hover { background: #168743; transform: translateY(-2px); }
.ghost-action, .outline-action { border: 1px solid rgba(22, 53, 42, .12); color: var(--profile-ink); background: rgba(255, 255, 255, .72); box-shadow: 0 8px 20px rgba(56, 111, 73, .04); }
.ghost-action:hover, .outline-action:hover { border-color: #7ab88e; background: #fff; }
.avatar-panel { display: flex; flex-direction: column; align-items: center; gap: 15px; }
.avatar-input { display: none; }
.avatar-button { position: relative; padding: 0; border: 0; background: transparent; cursor: pointer; border-radius: 50%; }
.avatar-button:disabled { cursor: wait; opacity: .7; }
.profile-avatar { border: 8px solid rgba(255, 255, 255, .82); box-shadow: 0 24px 50px rgba(30, 102, 60, .18); background: #ccebd5; color: #237346; font-size: 52px; }
.avatar-overlay { position: absolute; inset: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; border-radius: 50%; color: white; background: rgba(15, 53, 34, .68); opacity: 0; transition: opacity .25s ease; }
.avatar-button:hover .avatar-overlay { opacity: 1; }
.avatar-tip { color: #8aa496; font-size: 11px; }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 24px; margin-bottom: 30px; }
.heading-note, .section-heading p { color: #8aa496; font-size: 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.stat-card { position: relative; min-height: 180px; padding: 25px; border: 1px solid rgba(255, 255, 255, .78); border-radius: 22px; background: rgba(255, 255, 255, .84); box-shadow: 0 24px 60px rgba(31, 92, 58, .12), inset 0 1px 0 rgba(255, 255, 255, .78); backdrop-filter: blur(18px) saturate(115%); -webkit-backdrop-filter: blur(18px) saturate(115%); overflow: hidden; transition: transform .3s ease, box-shadow .3s ease; }
.stat-card::after { position: absolute; top: 0; left: -35%; width: 28%; height: 100%; content: ''; transform: skewX(-18deg); background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .45), transparent); opacity: 0; pointer-events: none; }
.stat-card:hover { transform: translateY(-5px); box-shadow: 0 30px 70px rgba(31, 92, 58, .16), inset 0 1px 0 rgba(255, 255, 255, .82); }
.stat-card:hover::after { opacity: 1; animation: card-glint .8s ease; }
@keyframes card-glint { from { left: -35%; } to { left: 125%; } }
.stat-label { color: var(--profile-muted); font-size: 13px; }
.stat-card strong { display: block; margin-top: 28px; color: #1b8b4c; font-size: 48px; line-height: 1; letter-spacing: -.06em; }
.stat-unit { display: block; margin-top: 8px; color: #8aa496; font-size: 13px; }
.stat-line { position: absolute; right: 24px; bottom: 25px; left: 24px; height: 2px; background: linear-gradient(90deg, #70c48c, transparent); }
.stats-detail-grid { display: grid; grid-template-columns: 1.08fr .92fr; gap: 16px; margin-top: 16px; }
.stats-chart-card, .stats-table-card { min-width: 0; padding: 24px; border: 1px solid rgba(255, 255, 255, .78); border-radius: 22px; background: rgba(255, 255, 255, .78); box-shadow: 0 20px 52px rgba(31, 92, 58, .1), inset 0 1px 0 rgba(255, 255, 255, .78); backdrop-filter: blur(18px) saturate(115%); -webkit-backdrop-filter: blur(18px) saturate(115%); }
.stats-panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.panel-kicker { color: #5a9b73; font-size: 10px; font-weight: 700; letter-spacing: .16em; }
.stats-panel-heading h3 { margin: 7px 0 0; color: var(--profile-ink); font-size: 21px; letter-spacing: -.04em; }
.panel-caption { color: #8aa496; font-size: 11px; text-align: right; }
.stats-chart-body { display: grid; grid-template-columns: 148px 1fr; align-items: center; gap: 28px; }
.completion-ring { display: grid; place-items: center; width: 148px; height: 148px; border-radius: 50%; box-shadow: 0 14px 30px rgba(29, 158, 82, .12); }
.completion-ring-inner { display: grid; place-items: center; width: 112px; height: 112px; border-radius: 50%; background: rgba(250, 253, 250, .92); }
.completion-ring-inner strong { color: #1d9e52; font-size: 30px; line-height: 1; letter-spacing: -.06em; }
.completion-ring-inner span { margin-top: -26px; color: #7b9887; font-size: 11px; }
.status-bars { display: grid; gap: 18px; }
.status-bar-row { display: grid; gap: 8px; }
.status-bar-label { display: flex; justify-content: space-between; gap: 12px; color: #668273; font-size: 12px; }
.status-bar-label strong { color: #1d8f4d; font-weight: 700; }
.status-bar-track { height: 8px; overflow: hidden; border-radius: 999px; background: rgba(29, 158, 82, .1); }
.status-bar-track span { display: block; height: 100%; min-width: 3px; border-radius: inherit; background: linear-gradient(90deg, #8bd09e, #1d9e52); transition: width 1s cubic-bezier(.22, 1, .36, 1); }
.stats-table { width: 100%; border-collapse: collapse; color: #5d7869; font-size: 12px; }
.stats-table tr { border-top: 1px solid rgba(22, 53, 42, .08); }
.stats-table tr:first-child { border-top: 0; }
.stats-table th, .stats-table td { padding: 13px 0; text-align: left; }
.stats-table th { color: #668273; font-weight: 500; }
.stats-table td:nth-child(2) { color: #1d8f4d; font-size: 17px; font-weight: 700; text-align: right; white-space: nowrap; }
.stats-table td small { margin-left: 4px; color: #8aa496; font-size: 10px; font-weight: 400; }
.stats-row-note { padding-left: 16px !important; color: #8aa496; font-size: 11px; text-align: right !important; white-space: nowrap; }
.journey-card { position: relative; padding: 34px 38px; border-radius: 24px; background: rgba(255, 255, 255, .84); border: 1px solid rgba(255, 255, 255, .78); box-shadow: 0 24px 60px rgba(31, 92, 58, .11), inset 0 1px 0 rgba(255, 255, 255, .78); backdrop-filter: blur(18px) saturate(115%); -webkit-backdrop-filter: blur(18px) saturate(115%); }
.journey-point { display: grid; grid-template-columns: 24px 1fr; gap: 18px; align-items: start; }
.point-dot { display: block; width: 13px; height: 13px; margin-top: 4px; border: 3px solid #b7dec1; border-radius: 50%; background: #fff; }
.point-dot.current { border-color: #1eac59; background: #1eac59; box-shadow: 0 0 0 6px rgba(30, 172, 89, .11); }
.journey-line { width: 1px; height: 58px; margin: 4px 0 4px 6px; background: #cfe4d4; }
.point-time { color: #7d9a88; font-size: 12px; letter-spacing: .08em; }
.journey-point h3 { margin: 8px 0 8px; font-size: 19px; }
.journey-point p { margin-bottom: 0; color: var(--profile-muted); line-height: 1.7; }
.works-heading { align-items: end; }
.works-heading p { margin-bottom: 0; }
.article-stream { display: flex; flex-direction: column; gap: 22px; }
.article-record { padding: 28px 30px 32px 68px; border: 1px solid rgba(255, 255, 255, .78); border-radius: 24px; background: rgba(255, 255, 255, .84); box-shadow: 0 24px 60px rgba(31, 92, 58, .11), inset 0 1px 0 rgba(255, 255, 255, .78); backdrop-filter: blur(18px) saturate(115%); -webkit-backdrop-filter: blur(18px) saturate(115%); }
.article-index { float: left; width: 38px; margin-left: -48px; color: #9cc5a7; font-size: 13px; }
.article-meta { display: flex; gap: 16px; color: #8aa496; font-size: 12px; }
.article-status { color: #239254; }
.article-title { display: flex; align-items: center; gap: 8px; margin: 15px 0 7px; padding: 0; border: 0; color: var(--profile-ink); background: transparent; font-size: 26px; font-weight: 700; letter-spacing: -.04em; cursor: pointer; text-align: left; }
.article-title:hover { color: #1d9e52; }
.article-title .article-title-icon-collapsed { transform: rotate(180deg); }
.article-title > .anticon { transition: transform .2s ease; }
.article-subtitle { color: #6e8b79; font-size: 14px; }
.article-body { color: #4d6657; font-size: 15px; line-height: 1.95; }
.article-body :deep(h1), .article-body :deep(h2), .article-body :deep(h3) { margin: 26px 0 10px; font-size: 22px; letter-spacing: -.03em; }
.article-body :deep(p) { margin: 0 0 14px; }
.article-body :deep(img) { max-width: 100%; border-radius: 14px; }
.article-body :deep(blockquote) { margin: 18px 0; padding: 12px 18px; border-left: 3px solid #78bd8c; background: #f2faf4; color: #64816e; }
.works-sentinel { display: flex; justify-content: center; padding: 24px; color: #8aa496; font-size: 12px; }
.state-card { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 240px; padding: 30px; border: 1px dashed #b7d5c0; border-radius: 24px; color: var(--profile-muted); text-align: center; background: rgba(255,255,255,.5); }
.state-card > .anticon { margin-bottom: 13px; color: #53a96f; font-size: 32px; }
.state-card h3 { margin-bottom: 5px; color: var(--profile-ink); }
.state-card p { margin-bottom: 18px; }
.state-card button { border: 0; color: #fff; background: #1d9e52; }
.error-state { color: #bd5a5a; }
.works-loading { display: flex; flex-direction: column; gap: 18px; }
.article-skeleton { min-height: 210px; padding: 28px; border-radius: 24px; background: linear-gradient(90deg, rgba(255,255,255,.55), rgba(232,245,235,.9), rgba(255,255,255,.55)); background-size: 240% 100%; animation: shimmer 1.6s infinite; }
.article-skeleton span, .article-skeleton i, .article-skeleton em { display: block; height: 12px; margin-bottom: 17px; border-radius: 8px; background: rgba(129, 181, 143, .28); }
.article-skeleton span { width: 25%; height: 10px; }.article-skeleton i { width: 60%; height: 24px; }.article-skeleton em { width: 92%; }.article-skeleton em:last-child { width: 74%; }
@keyframes shimmer { from { background-position: 100% 0; } to { background-position: -100% 0; } }
.edit-card { border: 1px solid rgba(255, 255, 255, .78); border-radius: 24px; background: rgba(255, 255, 255, .84); box-shadow: 0 24px 60px rgba(31, 92, 58, .11), inset 0 1px 0 rgba(255, 255, 255, .78); backdrop-filter: blur(18px) saturate(115%); -webkit-backdrop-filter: blur(18px) saturate(115%); }
.edit-section .section-content > * { opacity: 0; transform: translateY(14px); }
.edit-section.revealed .section-content > * { animation: edit-content-reveal .56s cubic-bezier(.22, 1, .36, 1) forwards; }
.edit-section.revealed .section-content > *:nth-child(2) { animation-delay: .08s; }
@keyframes edit-content-reveal { to { opacity: 1; transform: translateY(0); } }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0 20px; }
.full-field { grid-column: 1 / -1; }
.form-footer { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding-top: 10px; color: #8aa496; font-size: 12px; }
.save-button { border: 0; border-radius: 999px; background: #1d9e52; }

@media (max-width: 900px) {
  .profile-layout { display: block; padding-top: 16px; }
  .section-menu { position: sticky; top: 64px; flex-direction: row; align-items: center; gap: 4px; width: 100%; margin-bottom: 12px; padding: 7px; overflow-x: auto; }
  .menu-caption { display: none; }
  .menu-item { padding: 10px 14px; font-size: 13px; }
  .stats-detail-grid { grid-template-columns: 1fr; }
}

@media (max-width: 680px) {
  .profile-layout { padding: 0 16px 60px; }
  .hero-grid { grid-template-columns: 1fr; gap: 28px; }
  .hero-section { padding-top: 40px; }
  h1 { font-size: 52px; }
  .avatar-panel { order: -1; align-items: flex-start; }
  .hero-insight-grid { grid-template-columns: 1fr; max-width: 100%; }
  .stats-grid { grid-template-columns: 1fr; }
  .stats-chart-body { grid-template-columns: 1fr; justify-items: center; }
  .status-bars { width: 100%; }
  .stats-panel-heading { flex-direction: column; }
  .panel-caption { text-align: left; }
  .stats-row-note { display: none; }
  .profile-section, .stats-section, .history-section, .works-section, .edit-section { min-height: 0; padding: 52px 0; }
  .section-heading { align-items: flex-start; flex-direction: column; }
  .heading-note { display: none; }
  .article-record { padding: 24px; }
  .article-index { float: none; width: auto; margin: 0 0 14px; }
  .article-title { font-size: 22px; }
  .form-grid { grid-template-columns: 1fr; }
  .full-field { grid-column: auto; }
  .form-footer { align-items: flex-start; flex-direction: column; }
  .section-telemetry { right: 16px; bottom: 16px; }
  .section-telemetry strong { font-size: 24px; }
}

@media (prefers-reduced-motion: reduce) {
  .profile-section, .profile-section::before, .profile-section > :not(.section-loading), .profile-stage, .stage-layer, .avatar-overlay, .menu-item, .primary-action, .ghost-action, .outline-action, .stat-card, .section-telemetry strong { transition: none; }
  .article-skeleton, .loading-orbit, .stat-card:hover::after { animation: none; }
}
</style>
