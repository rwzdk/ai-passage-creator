<template>
  <div ref="root" class="stagger-list">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { gsap } from 'gsap'
import { useGsapMotion } from '@/composables/useGsapMotion'

const props = withDefaults(defineProps<{
  step?: number
  once?: boolean
  start?: string
}>(), {
  step: 110,
  once: true,
  start: 'top 82%',
})

const root = ref<HTMLElement | null>(null)

useGsapMotion(root, (element, reducedMotion) => {
  const children = Array.from(element.children)
  if (!children.length) return

  if (reducedMotion) {
    gsap.set(children, { clearProps: 'all' })
    return
  }

  gsap.fromTo(
    children,
    { autoAlpha: 0, y: 34, scale: 0.985 },
    {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: 0.88,
      stagger: props.step / 1000,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: element,
        start: props.start,
        once: props.once,
        toggleActions: props.once ? 'play none none none' : 'play none none reverse',
      },
    },
  )

})
</script>

<style scoped>
.stagger-list :deep(> *) {
  will-change: opacity, transform;
}
</style>
