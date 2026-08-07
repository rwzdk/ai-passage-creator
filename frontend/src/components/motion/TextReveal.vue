<template>
  <component
    :is="tag"
    ref="root"
    class="text-reveal"
    :class="{ 'is-visible': visible }"
    :aria-label="text"
  >
    <span
      v-for="(character, index) in characters"
      :key="`${character}-${index}`"
      class="text-reveal__character"
      :style="{ '--character-delay': `${index * step}ms` }"
      aria-hidden="true"
    >{{ character === ' ' ? '\u00a0' : character }}</span>
  </component>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(defineProps<{
  text: string
  tag?: string
  step?: number
  once?: boolean
}>(), {
  tag: 'p',
  step: 24,
  once: true,
})

const root = ref<HTMLElement | null>(null)
const visible = ref(false)
const characters = computed(() => Array.from(props.text))
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!root.value || typeof IntersectionObserver === 'undefined') {
    visible.value = true
    return
  }

  observer = new IntersectionObserver(([entry]) => {
    visible.value = entry.isIntersecting
    if (entry.isIntersecting && props.once) observer?.disconnect()
  }, { threshold: 0.2 })
  observer.observe(root.value)
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.text-reveal__character {
  display: inline-block;
  opacity: 0;
  transform: translate3d(0, 0.55em, 0) rotate(2deg);
  transition: opacity 520ms var(--ease-out), transform 520ms var(--ease-out);
  transition-delay: var(--character-delay);
}

.text-reveal.is-visible .text-reveal__character {
  opacity: 1;
  transform: translate3d(0, 0, 0) rotate(0);
}

@media (prefers-reduced-motion: reduce) {
  .text-reveal__character {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
