<template>
  <div class="vip-page">
    <div class="river-lines" aria-hidden="true">
      <span></span><span></span><span></span>
    </div>
    <div class="vip-container">
      <div class="page-header">
        <div class="eyebrow">AI PASSAGE CREATOR · MEMBERSHIP</div>
        <div class="header-row">
          <div>
            <div class="header-badge"><CrownOutlined /><span>创作者长期方案</span></div>
            <h1 class="page-title">让灵感，不再被配额打断</h1>
            <p class="page-subtitle">一次购买，解锁完整创作工作台。把更多时间留给选题、表达和下一篇作品。</p>
          </div>
          <div class="header-note">
            <span class="note-mark">01</span>
            <span>永久会员<br />一次支付 · 终身有效</span>
          </div>
        </div>
      </div>

      <div class="main-section">
        <div class="pricing-card">
          <div class="pricing-badge">创作者专享</div>
          <div class="pricing-header">
            <div class="plan-icon">
              <CrownOutlined />
            </div>
            <h2 class="plan-name">永久会员</h2>
            <p class="plan-copy">为持续写作准备的一张长期通行证</p>
            <div class="price-display">
              <span class="currency">$</span>
              <span class="price">199</span>
              <span class="period">/永久</span>
            </div>
            <div class="original-price">
              <span class="original-label">原价</span>
              <span class="original-value">$299</span>
            </div>
          </div>

          <div class="pricing-divider"></div>

          <div class="pricing-features">
            <div v-for="(item, index) in pricingFeatures" :key="index" class="pricing-feature">
              <CheckCircleOutlined class="feature-check" />
              <span>{{ item }}</span>
            </div>
          </div>

          <a-button
            type="primary"
            size="large"
            :loading="purchasing"
            :disabled="isVip"
            @click="handlePurchase"
            class="purchase-btn"
          >
            <template #icon>
              <ThunderboltOutlined />
            </template>
            {{ isVip ? '您已是永久会员' : '立即升级' }}
          </a-button>

          <div class="security-notice">
            <SafetyOutlined />
            <span>Stripe 安全托管 · 支持 7 天内退款</span>
          </div>
          <div class="checkout-steps" aria-label="支付流程">
            <div class="checkout-step"><span>1</span><p>点击升级</p></div>
            <i></i>
            <div class="checkout-step"><span>2</span><p>完成支付</p></div>
            <i></i>
            <div class="checkout-step"><span>3</span><p>立即解锁</p></div>
          </div>
        </div>

        <div class="features-section">
          <div class="section-header benefits-header">
            <div><span class="section-kicker">THE CREATOR'S TOOLKIT</span><h2 class="section-title">一套完整的创作支撑</h2></div>
            <span class="section-count">06 项权益</span>
          </div>
          <div class="features-grid">
            <div v-for="(feature, index) in features" :key="index" class="feature-card">
              <div class="feature-icon-wrapper">
                <component :is="feature.icon" class="feature-icon" />
              </div>
              <div class="feature-content">
                <h4 class="feature-title">{{ feature.title }}</h4>
                <p class="feature-desc">{{ feature.desc }}</p>
              </div>
            </div>
          </div>
          <div class="workflow-strip">
            <div class="workflow-intro">
              <span class="workflow-kicker">FROM IDEA TO ARTICLE</span>
              <strong>让每一步创作都更顺手</strong>
            </div>
            <div class="workflow-steps" aria-label="会员创作流程">
              <div class="workflow-step">
                <span class="workflow-index">01</span>
                <div>
                  <strong>AI 大纲规划</strong>
                  <p>先梳理结构，再开始写作</p>
                </div>
              </div>
              <span class="workflow-line" aria-hidden="true"></span>
              <div class="workflow-step">
                <span class="workflow-index">02</span>
                <div>
                  <strong>文章智能创作</strong>
                  <p>围绕主题生成完整内容</p>
                </div>
              </div>
              <span class="workflow-line" aria-hidden="true"></span>
              <div class="workflow-step">
                <span class="workflow-index">03</span>
                <div>
                  <strong>配图与图表合成</strong>
                  <p>补齐视觉表达，完成文章</p>
                </div>
              </div>
            </div>
          </div>
          <div class="feature-footnote"><span></span> 会员能力会持续跟随产品更新</div>
        </div>
      </div>

      <div class="faq-section">
        <div class="section-header">
          <div><span class="section-kicker">BEFORE YOU BEGIN</span><h2 class="section-title">购买前，先了解这些</h2></div>
          <QuestionCircleOutlined class="section-icon" />
        </div>
        <div class="faq-grid">
          <div v-for="(faq, index) in faqs" :key="index" :class="['faq-card', { open: openFaq === index }]">
            <button type="button" class="faq-question" @click="openFaq = openFaq === index ? -1 : index">
              <span>{{ faq.question }}</span><span class="faq-toggle">+</span>
            </button>
            <p v-if="openFaq === index" class="faq-answer">{{ faq.answer }}</p>
          </div>
        </div>
      </div>

      <p class="page-footer-note">在沅水边写下每一篇文章，也把每一次成长留在自己的时间线上。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CrownOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  RocketOutlined,
  PictureOutlined,
  AppstoreOutlined,
  EditOutlined,
  StarOutlined,
  GiftOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { createVipPaymentSession } from '@/api/paymentController'
import { isVip as checkIsVip } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const purchasing = ref(false)
const openFaq = ref<number | null>(0)

// 是否是 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 会员特权列表
const features = [
  {
    icon: RocketOutlined,
    title: '无限创作配额',
    desc: '无限次使用文章创作功能，告别配额限制'
  },
  {
    icon: PictureOutlined,
    title: 'AI 智能生图',
    desc: '使用 Nano Banana AI 生成独特配图'
  },
  {
    icon: AppstoreOutlined,
    title: 'SVG 图表生成',
    desc: '自动生成精美的概念示意图和思维导图'
  },
  {
    icon: EditOutlined,
    title: 'AI 大纲编辑',
    desc: '使用 AI 助手快速优化文章大纲'
  },
  {
    icon: StarOutlined,
    title: '优先队列',
    desc: '享受更快的生成速度和优先服务'
  },
  {
    icon: GiftOutlined,
    title: '终身有效',
    desc: '一次购买，永久使用，无需续费'
  }
]

// 价格卡片特性
const pricingFeatures = [
  '无限创作配额',
  '全部高级配图功能',
  'AI 大纲智能编辑',
  '优先生成队列',
  '终身有效'
]

// FAQ 列表
const faqs = [
  {
    question: '支付后多久生效？',
    answer: '支付成功后立即生效，您将立即获得永久会员权限，刷新页面即可看到变化。'
  },
  {
    question: '如何申请退款？',
    answer: '购买后 7 天内，如不满意可申请退款，退款后会员权限将被取消。'
  },
  {
    question: '会员是否需要续费？',
    answer: '不需要。永久会员一次购买，终身有效，无需任何续费。'
  },
  {
    question: '支付安全吗？',
    answer: '我们使用 Stripe 国际支付平台，全程加密传输，安全可靠。'
  }
]

// 检查支付结果
onMounted(async () => {
  const success = route.query.success
  const cancelled = route.query.cancelled

  if (success === 'true') {
    await loginUserStore.fetchLoginUser()
    Modal.success({
      title: '支付成功！',
      content: '恭喜您成为永久会员，已解锁全部高级功能！',
      okText: '开始创作',
      onOk: () => {
        router.push('/create')
      }
    })
    router.replace('/vip')
  } else if (cancelled === 'true') {
    message.info('支付已取消')
    router.replace('/vip')
  }
})

// 购买处理
const handlePurchase = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  if (isVip.value) {
    message.info('您已经是永久会员')
    return
  }

  purchasing.value = true
  try {
    const res = await createVipPaymentSession()
    if (res.data.code === 0 && res.data.data) {
      window.location.href = res.data.data
    } else {
      message.error(res.data.message || '创建支付失败')
    }
  } catch (error) {
    console.error('创建支付失败:', error)
    message.error('创建支付失败，请稀后重试')
  } finally {
    purchasing.value = false
  }
}
</script>

<style scoped lang="scss">
.vip-page {
  min-height: calc(100vh - 64px);
  background: var(--gradient-hero);
  padding: 48px 24px 80px;
}

.vip-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-dark);
  margin-bottom: 20px;

  .anticon {
    font-size: 14px;
  }
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--color-text);
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 主内容区 */
.main-section {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 32px;
  margin-bottom: 56px;
}

/* 价格卡片 */
.pricing-card {
  background: white;
  border-radius: var(--radius-xl);
  padding: 36px 32px;
  box-shadow: var(--shadow-xl);
  border: 2px solid var(--color-primary);
  position: relative;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.pricing-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--gradient-primary);
  color: white;
  padding: 6px 20px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  box-shadow: var(--shadow-green);
}

.pricing-header {
  text-align: center;
  padding-bottom: 20px;
}

.plan-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-lg);

  .anticon {
    font-size: 26px;
    color: var(--color-primary);
  }
}

.plan-name {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 14px;
  color: var(--color-text);
}

.price-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  margin-bottom: 6px;
}

.currency {
  font-size: 18px;
  color: var(--color-text-secondary);
  margin-right: 2px;
  font-weight: 500;
}

.price {
  font-size: 52px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.period {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-left: 4px;
}

.original-price {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
}

.original-label {
  color: var(--color-text-muted);
}

.original-value {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.pricing-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 20px 0;
}

.pricing-features {
  margin-bottom: 24px;
}

.pricing-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 14px;
  color: var(--color-text);

  .feature-check {
    color: var(--color-primary);
    font-size: 15px;
    flex-shrink: 0;
  }
}

.purchase-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  background: var(--gradient-primary) !important;
  border: none !important;
  box-shadow: var(--shadow-green) !important;
  border-radius: var(--radius-md) !important;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-background-tertiary) !important;
    color: var(--color-text-secondary) !important;
    box-shadow: none !important;
  }
}

.security-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--color-text-secondary);

  .anticon {
    color: var(--color-primary);
    font-size: 13px;
  }
}

/* 会员特权 */
.features-section {
  background: white;
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--color-border);
}

.features-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 24px;
  color: var(--color-text);

  .anticon {
    color: var(--color-primary);
    font-size: 20px;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);

  &:hover {
    background: rgba(34, 197, 94, 0.06);
  }
}

.feature-icon-wrapper {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-md);
}

.feature-icon {
  font-size: 18px;
  color: var(--color-primary);
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--color-text);
}

.feature-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

/* FAQ 部分 */
.faq-section {
  background: white;
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.section-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--color-text);
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.faq-card {
  padding: 20px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
}

.faq-question {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--color-text);
}

.faq-answer {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* 响应式 */
@media (max-width: 992px) {
  .main-section {
    grid-template-columns: 1fr;
  }

  .pricing-card {
    position: static;
    max-width: 400px;
    margin: 0 auto;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .faq-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .vip-page {
    padding: 32px 16px 60px;
  }

  .page-title {
    font-size: 28px;
  }

  .page-subtitle {
    font-size: 14px;
  }

  .pricing-card {
    padding: 28px 24px;
  }

  .price {
    font-size: 44px;
  }

  .features-section,
  .faq-section {
    padding: 24px;
  }
}
/* 沅水青山会员页：用轻量的水纹与纸面层次承接全局视觉 */
.vip-page {
  position: relative;
  overflow: hidden;
  min-height: calc(100vh - 64px);
  padding: 66px 24px 84px;
  background:
    radial-gradient(circle at 78% 12%, rgba(199, 168, 120, .16), transparent 28%),
    linear-gradient(160deg, #e6f0ea 0%, #f7f5ee 48%, #e2eee8 100%);
}

.vip-container { position: relative; z-index: 1; max-width: 1180px; }
.river-lines { position: absolute; inset: 0; pointer-events: none; opacity: .34; }
.river-lines span { position: absolute; display: block; width: 65vw; height: 18vw; border: 1px solid rgba(69, 111, 100, .18); border-radius: 50%; transform: rotate(-12deg); animation: river-drift 12s ease-in-out infinite alternate; }
.river-lines span:nth-child(1) { right: -16vw; top: 8%; }
.river-lines span:nth-child(2) { left: -22vw; top: 48%; width: 72vw; animation-delay: -4s; }
.river-lines span:nth-child(3) { right: -20vw; bottom: 6%; width: 60vw; animation-delay: -8s; }
@keyframes river-drift { from { transform: rotate(-12deg) translate3d(-14px, 0, 0); } to { transform: rotate(-9deg) translate3d(14px, 10px, 0); } }

.page-header { position: relative; margin-bottom: 42px; text-align: left; }
.vip-page .page-header { margin: 0 0 30px; padding: 0 0 18px; background: transparent; }
.eyebrow, .section-kicker { color: var(--mountain-green); font-size: 11px; font-weight: 700; letter-spacing: .18em; }
.header-row { display: flex; align-items: end; justify-content: space-between; gap: 32px; margin-top: 18px; }
.header-badge { display: inline-flex; align-items: center; gap: 7px; margin-bottom: 14px; padding: 7px 12px; border: 1px solid rgba(69, 111, 100, .2); border-radius: 999px; color: var(--mountain-green); background: rgba(247, 250, 246, .62); font-size: 12px; }
.page-title { max-width: 760px; margin: 0 0 14px; color: var(--ink-deep); font-size: clamp(36px, 5vw, 62px); font-weight: 600; line-height: 1.08; letter-spacing: -.04em; }
.page-subtitle { max-width: 600px; margin: 0; color: var(--ink-muted); font-size: 16px; line-height: 1.8; }
.header-note { display: flex; align-items: center; gap: 12px; padding-bottom: 7px; color: var(--ink-muted); font-size: 12px; line-height: 1.7; text-align: right; white-space: nowrap; }
.note-mark { color: var(--accent-gold); font-size: 36px; font-weight: 300; letter-spacing: -.08em; }

.main-section { grid-template-columns: 380px minmax(0, 1fr); gap: 24px; align-items: stretch; margin-bottom: 28px; }
.pricing-card, .features-section, .faq-section { border: 1px solid rgba(255, 255, 255, .78); background: rgba(247, 250, 246, .72); box-shadow: 0 26px 80px rgba(32, 59, 56, .11), inset 0 1px 0 rgba(255, 255, 255, .74); backdrop-filter: blur(20px) saturate(115%); -webkit-backdrop-filter: blur(20px) saturate(115%); }
.pricing-card { position: sticky; top: 88px; padding: 36px 32px 28px; border: 1px solid rgba(255, 255, 255, .84); border-radius: 24px; }
.pricing-badge { background: var(--ink-deep); box-shadow: 0 10px 24px rgba(32, 59, 56, .18); }
.plan-icon { border-radius: 50%; background: rgba(199, 168, 120, .17); }
.plan-icon .anticon { color: var(--accent-gold); }
.plan-name { color: var(--ink-deep); }
.plan-copy { margin: -6px 0 18px; color: var(--ink-muted); font-size: 12px; }
.price { color: var(--ink-deep); font-weight: 500; }
.currency, .period, .original-label, .original-value { color: var(--ink-muted); }
.pricing-divider { background: var(--line-soft); }
.pricing-feature { color: var(--ink-deep); }
.pricing-feature .feature-check { color: var(--mountain-green); }
.purchase-btn { background: var(--ink-deep) !important; box-shadow: 0 12px 24px rgba(32, 59, 56, .2) !important; border-radius: 999px !important; transition: transform var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast); }
.purchase-btn:hover:not(:disabled) { opacity: 1; background: var(--mountain-green) !important; transform: translateY(-2px); box-shadow: 0 16px 28px rgba(69, 111, 100, .24) !important; }
.security-notice { color: var(--ink-muted); }
.security-notice .anticon { color: var(--mountain-green); }
.checkout-steps { display: flex; align-items: flex-start; gap: 8px; margin-top: 26px; padding-top: 20px; border-top: 1px solid var(--line-soft); }
.checkout-steps > i { flex: 1; height: 1px; margin-top: 12px; background: rgba(69, 111, 100, .2); }
.checkout-step { display: flex; flex-direction: column; align-items: center; gap: 6px; min-width: 54px; color: var(--ink-muted); font-size: 11px; text-align: center; }
.checkout-step span { display: grid; place-items: center; width: 24px; height: 24px; border: 1px solid rgba(69, 111, 100, .3); border-radius: 50%; color: var(--mountain-green); background: rgba(255, 255, 255, .62); }
.checkout-step p { margin: 0; }

.features-section { display: flex; min-height: 100%; flex-direction: column; padding: 32px; border-radius: 24px; }
.benefits-header { justify-content: space-between; align-items: end; margin-bottom: 24px; }
.section-title { margin: 8px 0 0; color: var(--ink-deep); font-size: 24px; font-weight: 600; letter-spacing: -.04em; }
.section-count { color: var(--ink-muted); font-size: 12px; }
.features-grid { flex: 1; grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-rows: repeat(3, minmax(112px, 1fr)); gap: 14px; align-content: stretch; }
.feature-card { align-items: center; min-height: 0; padding: 20px 18px; border: 1px solid rgba(255, 255, 255, .62); border-radius: 16px; background: rgba(255, 255, 255, .48); transition: transform var(--transition-normal), border-color var(--transition-normal), background var(--transition-normal); }
.feature-card:hover { border-color: rgba(69, 111, 100, .2); background: rgba(255, 255, 255, .82); transform: translateY(-3px); }
.feature-icon-wrapper { width: 36px; height: 36px; border-radius: 10px; background: rgba(143, 184, 164, .2); }
.feature-icon, .features-title .anticon { color: var(--mountain-green); }
.feature-title { color: var(--ink-deep); }
.feature-desc { color: var(--ink-muted); }
.feature-footnote { margin-top: 20px; color: var(--ink-muted); font-size: 12px; }
.feature-footnote span { display: inline-block; width: 6px; height: 6px; margin-right: 6px; border-radius: 50%; background: var(--accent-gold); box-shadow: 0 0 0 5px rgba(199, 168, 120, .12); }
.workflow-strip { display: flex; align-items: center; gap: 24px; margin-top: 24px; padding: 18px 0 4px; border-top: 1px solid var(--line-soft); }
.workflow-intro { display: flex; flex: 0 0 145px; flex-direction: column; gap: 5px; color: var(--ink-deep); }
.workflow-kicker { color: var(--mountain-green); font-size: 9px; font-weight: 700; letter-spacing: .16em; }
.workflow-intro strong { font-size: 13px; font-weight: 600; line-height: 1.45; }
.workflow-steps { display: flex; flex: 1; align-items: center; gap: 12px; min-width: 0; }
.workflow-step { display: flex; flex: 1; align-items: flex-start; gap: 9px; min-width: 0; }
.workflow-index { flex-shrink: 0; color: var(--accent-gold); font-size: 11px; font-weight: 700; line-height: 1.5; }
.workflow-step strong { display: block; color: var(--ink-deep); font-size: 12px; font-weight: 600; line-height: 1.45; }
.workflow-step p { margin: 3px 0 0; color: var(--ink-muted); font-size: 11px; line-height: 1.45; }
.workflow-line { flex: 0 0 20px; height: 1px; margin-top: 8px; background: rgba(69, 111, 100, .24); }

.faq-section { padding: 28px 32px; border-radius: 20px; }
.faq-section .section-header { justify-content: space-between; align-items: end; margin-bottom: 22px; }
.section-icon { color: var(--accent-gold); }
.faq-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 26px; }
.faq-card { padding: 0; border-bottom: 1px solid var(--line-soft); border-radius: 0; background: transparent; }
.faq-question { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 17px 0; border: 0; color: var(--ink-deep); background: transparent; font-size: 14px; font-weight: 600; text-align: left; cursor: pointer; }
.faq-toggle { color: var(--mountain-green); font-size: 22px; font-weight: 300; transition: transform var(--transition-fast); }
.faq-card.open .faq-toggle { transform: rotate(45deg); }
.faq-answer { margin: -4px 26px 16px 0; color: var(--ink-muted); font-size: 13px; line-height: 1.75; }
.page-footer-note { margin: 28px 0 0; color: var(--ink-muted); font-size: 12px; text-align: center; letter-spacing: .08em; }

@media (max-width: 992px) {
  .header-row { align-items: flex-start; flex-direction: column; gap: 18px; }
  .header-note { text-align: left; }
  .main-section { grid-template-columns: 1fr; }
  .pricing-card { position: relative; top: auto; max-width: 520px; width: 100%; margin: 0 auto; }
  .features-grid { flex: none; grid-template-rows: none; }
}

@media (max-width: 680px) {
  .vip-page { padding: 40px 16px 60px; }
  .page-header { margin-bottom: 28px; }
  .page-title { font-size: 38px; }
  .page-subtitle { font-size: 14px; }
  .pricing-card, .features-section, .faq-section { padding: 24px 20px; border-radius: 16px; }
  .features-grid, .faq-grid { grid-template-columns: 1fr; }
  .features-grid { grid-template-rows: none; }
  .workflow-strip { align-items: flex-start; flex-direction: column; gap: 14px; }
  .workflow-intro { flex-basis: auto; }
  .workflow-steps { width: 100%; align-items: stretch; flex-direction: column; gap: 12px; }
  .workflow-line { display: none; }
  .price { font-size: 46px; }
  .checkout-steps { gap: 4px; }
}

@media (prefers-reduced-motion: reduce) {
  .river-lines span, .feature-card, .purchase-btn, .faq-toggle { animation: none; transition: none; }
}
/* 深色沅水背景承接会员页面的长期陪伴感 */
.vip-page {
  background-image:
    linear-gradient(160deg, rgba(32, 59, 56, .62), rgba(247, 245, 238, .72) 65%),
    url('@/assets/scenes/vip-river-dawn.png');
  background-position: center;
  background-size: cover;
  background-attachment: fixed;
}

.vip-page .page-header::before {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(69, 111, 100, .3), transparent);
  content: '';
}

@media (max-width: 768px) {
  .vip-page { background-attachment: scroll; background-position: 62% center; }
}
</style>
