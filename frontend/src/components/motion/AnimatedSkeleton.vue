<template>
  <div class="animated-skeleton" :style="{ '--skeleton-lines': lines }" aria-hidden="true">
    <span v-for="line in lines" :key="line" />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ lines?: number }>(), { lines: 3 })
</script>

<style scoped>
.animated-skeleton {
  display: grid;
  gap: 10px;
}

.animated-skeleton span {
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(143, 184, 164, 0.14), rgba(143, 184, 164, 0.36), rgba(143, 184, 164, 0.14));
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s linear infinite;
}

.animated-skeleton span:nth-child(2n) {
  width: 84%;
}

.animated-skeleton span:nth-child(3n) {
  width: 66%;
}

@keyframes skeleton-wave {
  to {
    background-position: -200% 0;
  }
}
</style>
