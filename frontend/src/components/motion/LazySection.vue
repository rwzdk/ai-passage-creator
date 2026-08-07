<template>
  <section
    ref="root"
    :class="['lazy-section', { 'is-ready': ready }]"
    :aria-busy="!ready"
  >
    <div v-if="!ready && showSkeleton" class="lazy-section-skeleton" aria-hidden="true">
      <span />
      <span />
      <span />
    </div>
    <div v-show="ready" class="lazy-section-content">
      <slot :ready="ready" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    rootMargin?: string
    showSkeleton?: boolean
  }>(),
  { rootMargin: '160px 0px', showSkeleton: true },
)

const root = ref<HTMLElement | null>(null)
const ready = ref(false)
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!root.value || typeof IntersectionObserver === 'undefined') {
    ready.value = true
    return
  }

  observer = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) {
        ready.value = true
        observer?.disconnect()
      }
    },
    { rootMargin: props.rootMargin, threshold: 0 },
  )
  observer.observe(root.value)
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.lazy-section {
  min-height: 36px;
}

.lazy-section-content {
  animation: lazy-content-in 560ms var(--ease-out) both;
}

.lazy-section-skeleton {
  display: grid;
  gap: 10px;
  padding: 16px 0;
}

.lazy-section-skeleton span {
  display: block;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(143, 184, 164, 0.15), rgba(143, 184, 164, 0.34), rgba(143, 184, 164, 0.15));
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s linear infinite;
}

.lazy-section-skeleton span:nth-child(2) {
  width: 82%;
}

.lazy-section-skeleton span:nth-child(3) {
  width: 64%;
}

@keyframes lazy-content-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes skeleton-wave {
  to {
    background-position: -200% 0;
  }
}
</style>
