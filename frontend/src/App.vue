<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

provide('locale', zhCN)
</script>

<template>
  <a-config-provider :locale="zhCN">
    <div class="app-shell">
      <BasicLayout />
    </div>
  </a-config-provider>
</template>

<style>
.app-shell {
  min-height: 100vh;
  isolation: isolate;
}
</style>
