<template>
  <div class="basic-layout page-shell">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <main class="main-content">
      <router-view />
    </main>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </div>
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

:deep(.article-create-page),
:deep(.article-list-page),
:deep(.article-detail-page),
:deep(#userManagePage),
:deep(.statistics-page) {
  padding-top: 88px;
}

@media (max-width: 768px) {
  :deep(.article-create-page),
  :deep(.article-list-page),
  :deep(.article-detail-page),
  :deep(#userManagePage),
  :deep(.statistics-page) {
    padding-top: 72px;
  }
}
</style>
