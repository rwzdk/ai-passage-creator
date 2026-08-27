<template>
  <footer class="footer" :class="{ 'footer--visible': isFooterVisible }">
    <div class="ink-wash" aria-hidden="true">
      <svg viewBox="0 0 1440 450" preserveAspectRatio="none" focusable="false">
        <path
          class="wash wash-back"
          d="M-40 142C166 75 284 203 488 150c155-40 275-171 437-120 190 60 307 14 555-35v274H-40Z"
        />
        <path
          class="wash wash-front"
          d="M-30 283c177-99 347-3 524-76 196-82 307-40 446 15 190 75 309-58 535 2v246H-30Z"
        />
      </svg>
    </div>

    <div ref="footerRef" class="footer-content">
      <p class="footer-kicker">CONTACT - 让每一段灵感，都成为可以抵达的文字</p>

      <div class="wordmark" aria-label="MindOfqc">
        <svg class="wordmark-svg" viewBox="0 0 820 220" role="img">
          <text x="410" y="128" text-anchor="middle" class="wordmark-shadow">MindOfqc</text>
          <text x="410" y="128" text-anchor="middle" class="wordmark-stroke">MindOfqc</text>
          <text x="410" y="190" text-anchor="middle" class="wordmark-subtitle">YUANJIAN STUDIO</text>
        </svg>
      </div>

      <p class="contact-email">mindofqc.me</p>

      <nav class="social-links" aria-label="个人主页">
        <template v-for="link in socialLinks" :key="link.label">
          <a
            v-if="link.href"
            :href="link.href"
            target="_blank"
            rel="noopener noreferrer"
            class="social-link"
          >
            {{ link.label }}
          </a>
          <span v-else class="social-link social-link-pending" title="待配置个人主页链接">
            {{ link.label }}
          </span>
        </template>
      </nav>
    </div>

    <div class="footer-meta">
      <span>YUANJIAN STUDIO</span>
      <span>© {{ currentYear }} 沅笺</span>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const currentYear = new Date().getFullYear()
const footerRef = ref<HTMLElement | null>(null)
const isFooterVisible = ref(false)
let footerObserver: IntersectionObserver | null = null
let replayFrame = 0

onMounted(() => {
  if (!footerRef.value || typeof IntersectionObserver === 'undefined') {
    isFooterVisible.value = true
    return
  }

  footerObserver = new IntersectionObserver(
    ([entry]) => {
      cancelAnimationFrame(replayFrame)
      if (!entry.isIntersecting) {
        isFooterVisible.value = false
        return
      }

      // 移除动画类后在下一帧重新添加，确保每次进入都从首笔重新描绘。
      isFooterVisible.value = false
      replayFrame = requestAnimationFrame(() => {
        isFooterVisible.value = true
      })
    },
    { threshold: 0.22 },
  )
  footerObserver.observe(footerRef.value)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(replayFrame)
  footerObserver?.disconnect()
})

// 在此填写个人主页地址后，对应入口会自动启用跳转。
const socialLinks = [
  {
    label: '哔哩哔哩',
    href: 'https://space.bilibili.com/3546701390416086?spm_id_from=333.1007.0.0',
  },
  { label: 'GitHub', href: 'https://github.com/rwzdk' },
  { label: '知乎', href: 'https://www.zhihu.com/people/rwzdk' },
]
</script>

<style scoped>
.footer {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  min-height: 530px;
  margin-top: 0;
  padding: 70px 24px 20px;
  color: var(--ink-deep);
  background: #e9eee7;
  border: 0;
  text-align: center;
}

.footer::before {
  position: absolute;
  z-index: -2;
  inset: 0;
  content: '';
  opacity: 0.46;
  background-image: repeating-linear-gradient(
    0deg,
    rgba(32, 59, 56, 0.025) 0 1px,
    transparent 1px 4px
  );
  pointer-events: none;
}

.ink-wash {
  position: absolute;
  z-index: -1;
  inset: 0;
  height: auto;
  overflow: hidden;
  opacity: 0.7;
  pointer-events: none;
}

.ink-wash svg {
  width: 100%;
  height: 100%;
}

.wash {
  fill: var(--river-green);
}
.wash-back {
  opacity: 0.16;
  filter: blur(10px);
}
.wash-front {
  fill: var(--mountain-green);
  opacity: 0.09;
  filter: blur(17px);
}

.footer-content {
  display: flex;
  position: relative;
  z-index: 1;
  align-items: center;
  flex-direction: column;
  max-width: 1060px;
  margin: 0 auto;
}

.footer-kicker {
  margin: 0;
  color: rgba(32, 59, 56, 0.58);
  font-family: 'Work Sans', sans-serif;
  font-size: 11px;
  letter-spacing: 0.22em;
}

.wordmark {
  width: min(100%, 680px);
  margin-top: 38px;
}
.wordmark-svg {
  display: block;
  width: 100%;
  overflow: visible;
}

.wordmark-shadow,
.wordmark-stroke {
  font-family: 'Segoe Script', 'Brush Script MT', cursive;
  font-size: 128px;
  font-weight: 600;
  letter-spacing: 0;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.wordmark-shadow {
  fill: transparent;
  stroke: rgba(32, 59, 56, 0.1);
  stroke-width: 0.8px;
  opacity: 0;
}

.wordmark-stroke {
  fill: transparent;
  stroke: rgba(32, 59, 56, 0.72);
  stroke-width: 1.6px;
  stroke-dasharray: 1500;
  stroke-dashoffset: 1500;
  paint-order: stroke fill;
}

.wordmark-subtitle {
  display: block;
  margin-top: 16px;
  color: rgba(32, 59, 56, 0.42);
  font-family: 'Work Sans', sans-serif;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.32em;
  opacity: 0;
}

.footer--visible .wordmark-subtitle {
  animation: fade-up 700ms 1.55s var(--ease-out) forwards;
}

.footer--visible .wordmark-stroke {
  animation:
    draw-wordmark 2.8s 180ms cubic-bezier(0.38, 0, 0.16, 1) forwards,
    reveal-fill 680ms 2.15s ease-out forwards;
}

.footer--visible .wordmark-shadow {
  animation: reveal-ink-shadow 620ms 2.25s ease-out forwards;
}

.contact-email {
  margin: 44px 0 0;
  color: var(--ink-deep);
  font-family: 'Work Sans', sans-serif;
  font-size: 20px;
  letter-spacing: 0.08em;
}
.contact-email:focus-visible,
.social-link:focus-visible {
  outline: 2px solid var(--accent-gold);
  outline-offset: 5px;
}

.social-links {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px 42px;
  margin-top: 52px;
}

.social-link {
  color: rgba(32, 59, 56, 0.62);
  font-family: 'Work Sans', sans-serif;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-decoration: none;
  transition:
    color var(--transition-fast),
    transform var(--transition-fast);
}

a.social-link:hover {
  color: var(--ink-deep);
  transform: translateY(-2px);
}
.social-link-pending {
  cursor: default;
}

.footer-meta {
  display: flex;
  position: absolute;
  right: 24px;
  bottom: 18px;
  left: 24px;
  justify-content: space-between;
  color: rgba(32, 59, 56, 0.5);
  font-family: 'Work Sans', sans-serif;
  font-size: 10px;
  letter-spacing: 0.2em;
}

@keyframes draw-wordmark {
  to { stroke-dashoffset: 0; }
}
@keyframes reveal-fill {
  to { fill: rgba(32, 59, 56, 0.56); }
}
@keyframes reveal-ink-shadow {
  to { fill: rgba(32, 59, 56, 0.14); opacity: 1; }
}
@keyframes fade-up {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .footer {
    min-height: 500px;
    padding: 58px 20px 18px;
  }
  .footer-kicker {
    max-width: 270px;
    font-size: 10px;
    line-height: 1.8;
    letter-spacing: 0.14em;
  }
  .wordmark {
    margin-top: 34px;
  }
  .wordmark-shadow,
  .wordmark-stroke {
    font-size: 98px;
  }
  .wordmark-subtitle {
    font-size: 12px;
    letter-spacing: 0.22em;
  }
  .contact-email {
    margin-top: 36px;
    font-size: 16px;
  }
  .social-links {
    gap: 14px 22px;
    margin-top: 42px;
  }
  .footer-meta {
    bottom: 15px;
    flex-direction: column;
    gap: 6px;
    align-items: center;
    font-size: 9px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wordmark-stroke {
    stroke-dashoffset: 0;
    animation: none;
  }
  .wordmark-shadow {
    fill: rgba(32, 59, 56, 0.14);
    opacity: 1;
    animation: none;
  }
  .wordmark-subtitle {
    opacity: 1;
    animation: none;
  }
  .contact-email,
  .social-link {
    transition: none;
  }
}
</style>
