<template>
  <span ref="root" class="count-up-number" aria-live="polite">{{ displayedValue }}{{ suffix }}</span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    duration?: number
    suffix?: string
    replayOnView?: boolean
  }>(),
  { duration: 900, suffix: '', replayOnView: false },
)

const root = ref<HTMLElement | null>(null)
const displayed = ref(0)
const targetValue = ref(0)
const inView = ref(false)
let frame = 0
let observer: IntersectionObserver | null = null

const displayedValue = computed(() => Math.round(displayed.value).toLocaleString('zh-CN'))

function animate(target: number, reset = false) {
  cancelAnimationFrame(frame)
  if (reset) displayed.value = 0
  const start = displayed.value
  const startTime = performance.now()

  const tick = (now: number) => {
    const progress = Math.min(1, (now - startTime) / props.duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    displayed.value = start + (target - start) * eased
    if (progress < 1) frame = requestAnimationFrame(tick)
  }

  frame = requestAnimationFrame(tick)
}

watch(() => props.value, (value) => {
  targetValue.value = Number.isFinite(value) ? value : 0
  if (!props.replayOnView || inView.value) animate(targetValue.value, props.replayOnView)
}, { immediate: true })

onMounted(() => {
  if (!props.replayOnView || !root.value || typeof IntersectionObserver === 'undefined') {
    inView.value = true
    return
  }

  observer = new IntersectionObserver(([entry]) => {
    inView.value = entry.isIntersecting
    if (entry.isIntersecting) animate(targetValue.value, true)
  }, { threshold: 0.35 })
  observer.observe(root.value)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  observer?.disconnect()
})
</script>

<style scoped>
.count-up-number {
  font-variant-numeric: tabular-nums;
}
</style>
