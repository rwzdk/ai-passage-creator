<template>
  <div class="stagger-list" :style="{ '--stagger-step': `${step}ms` }">
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ step?: number }>(), { step: 70 })
</script>

<style scoped>
.stagger-list :deep(> *) {
  animation: stagger-in 560ms var(--ease-out) both;
}

.stagger-list :deep(> *:nth-child(1)) { animation-delay: calc(var(--stagger-step) * 0); }
.stagger-list :deep(> *:nth-child(2)) { animation-delay: calc(var(--stagger-step) * 1); }
.stagger-list :deep(> *:nth-child(3)) { animation-delay: calc(var(--stagger-step) * 2); }
.stagger-list :deep(> *:nth-child(4)) { animation-delay: calc(var(--stagger-step) * 3); }
.stagger-list :deep(> *:nth-child(5)) { animation-delay: calc(var(--stagger-step) * 4); }
.stagger-list :deep(> *:nth-child(6)) { animation-delay: calc(var(--stagger-step) * 5); }

@keyframes stagger-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
