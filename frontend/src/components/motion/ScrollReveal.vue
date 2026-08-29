<template>
  <div
    ref="root"
    class="scroll-reveal"
    :style="{ '--reveal-delay': `${delay}ms` }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { gsap } from 'gsap'
import { useGsapMotion } from '@/composables/useGsapMotion'

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
useGsapMotion(root, (element, reducedMotion) => {
  if (reducedMotion) {
    gsap.set(element, { clearProps: 'all' })
    return
  }

  gsap.fromTo(
    element,
    { autoAlpha: 0, y: 30, scale: 0.985 },
    {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: 0.88,
      delay: props.delay / 1000,
      ease: 'power3.out',
      onStart: () => {
        element.style.willChange = 'transform, opacity'
      },
      onComplete: () => {
        element.style.willChange = ''
      },
      onReverseComplete: () => {
        element.style.willChange = ''
      },
      scrollTrigger: {
        trigger: element,
        start: `top ${props.rootMargin.includes('-') ? '84%' : '88%'}`,
        once: props.once,
        toggleActions: props.once ? 'play none none none' : 'play none none reverse',
      },
    },
  )
})
</script>

<style scoped>
</style>
