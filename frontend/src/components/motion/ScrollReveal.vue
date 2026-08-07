<template>
  <div
    ref="root"
    :class="['scroll-reveal', { 'is-visible': visible }]"
    :style="{ '--reveal-delay': `${delay}ms` }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    delay?: number
    once?: boolean
    rootMargin?: string
  }>(),
  {
    delay: 0,
    once: false,
    rootMargin: '0px 0px -10% 0px',
  },
)

const root = ref<HTMLElement | null>(null)
const visible = ref(false)
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!root.value || typeof IntersectionObserver === 'undefined') {
    visible.value = true
    return
  }

  observer = new IntersectionObserver(
    ([entry]) => {
      visible.value = entry.isIntersecting
      if (entry.isIntersecting && props.once) {
        observer?.disconnect()
      }
    },
    { rootMargin: props.rootMargin, threshold: 0.08 },
  )
  observer.observe(root.value)
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.scroll-reveal {
  opacity: 0;
  transform: translate3d(0, 24px, 0);
  transition:
    opacity 680ms var(--ease-out),
    transform 680ms var(--ease-out);
  transition-delay: var(--reveal-delay);
  will-change: opacity, transform;
}

.scroll-reveal.is-visible {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}
</style>
