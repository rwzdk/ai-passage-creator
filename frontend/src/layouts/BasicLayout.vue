<template>
  <a-layout class="basic-layout page-shell">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in" appear>
          <component :is="Component" />
        </Transition>
      </router-view>
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: var(--mist-white);
}

.main-content {
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}

.page-enter-active,
.page-leave-active {
  transition:
    opacity 360ms var(--ease-out),
    transform 360ms var(--ease-out),
    filter 360ms var(--ease-out);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(14px);
  filter: blur(4px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  filter: blur(3px);
}

@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active {
    transition: opacity 120ms linear;
  }

  .page-enter-from,
  .page-leave-to {
    transform: none;
    filter: none;
  }
}
</style>
