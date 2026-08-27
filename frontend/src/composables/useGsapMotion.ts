import { onBeforeUnmount, onMounted, type Ref } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

type MotionSetup = (root: HTMLElement, reducedMotion: boolean) => void

export function useGsapMotion(root: Ref<HTMLElement | null>, setup: MotionSetup) {
  let context: gsap.Context | null = null

  onMounted(() => {
    if (!root.value) return

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    context = gsap.context(() => setup(root.value as HTMLElement, reducedMotion), root.value)
  })

  onBeforeUnmount(() => {
    context?.revert()
    context = null
  })

  return {
    refresh: () => ScrollTrigger.refresh(),
  }
}
