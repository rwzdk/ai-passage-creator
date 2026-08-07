<template>
  <div class="scene-background" aria-hidden="true">
    <div
      v-for="layer in layers"
      :key="layer.id"
      :class="['scene-background-layer', { active: layer.active, loaded: layer.loaded }]"
      :style="{ backgroundImage: layer.loaded ? `url(${layer.background})` : undefined }"
    />
    <div class="scene-background-wash" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    background: string
    active?: boolean
  }>(),
  { active: true },
)

type BackgroundLayer = { id: number; background: string; active: boolean; loaded: boolean }
const nextId = ref(0)
const layers = ref<BackgroundLayer[]>([])
let image: HTMLImageElement | null = null

const current = computed(() => layers.value.find((layer) => layer.active))

function preload(background: string) {
  image?.decode?.().catch(() => undefined)
  image = new Image()
  image.onload = () => {
    const layer = layers.value.find((item) => item.background === background)
    if (layer) layer.loaded = true
  }
  image.src = background
}

watch(
  () => props.background,
  (background) => {
    const existing = layers.value.find((layer) => layer.background === background)
    if (existing) {
      layers.value = layers.value.map((layer) => ({ ...layer, active: layer.id === existing.id }))
      return
    }

    const id = nextId.value++
    layers.value = [
      ...layers.value.map((layer) => ({ ...layer, active: false })),
      { id, background, active: true, loaded: false },
    ].slice(-2)
    preload(background)
  },
  { immediate: true },
)

watch(() => props.active, (active) => {
  if (!active && current.value) current.value.active = false
})

onBeforeUnmount(() => {
  if (image) image.onload = null
  image = null
})
</script>

<style scoped>
.scene-background,
.scene-background-layer,
.scene-background-wash {
  position: absolute;
  inset: 0;
}

.scene-background {
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--gradient-hero);
}

.scene-background-layer {
  background-position: center;
  background-size: cover;
  opacity: 0;
  transform: scale(1.035);
  transition:
    opacity 720ms var(--ease-out),
    transform 1600ms var(--ease-out);
}

.scene-background-layer.loaded.active {
  opacity: 1;
  transform: scale(1);
}

.scene-background-wash {
  background: linear-gradient(90deg, rgba(247, 245, 238, 0.88) 0%, rgba(247, 245, 238, 0.58) 43%, rgba(247, 245, 238, 0.2) 100%);
}
</style>
