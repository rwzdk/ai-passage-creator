<template>
  <span class="count-up-number" aria-live="polite">{{ displayedValue }}{{ suffix }}</span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    duration?: number
    suffix?: string
  }>(),
  { duration: 900, suffix: '' },
)

const displayed = ref(0)
let frame = 0

const displayedValue = computed(() => Math.round(displayed.value).toLocaleString('zh-CN'))

function animate(target: number) {
  cancelAnimationFrame(frame)
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

watch(() => props.value, (value) => animate(Number.isFinite(value) ? value : 0), { immediate: true })
onBeforeUnmount(() => cancelAnimationFrame(frame))
</script>

<style scoped>
.count-up-number {
  font-variant-numeric: tabular-nums;
}
</style>
