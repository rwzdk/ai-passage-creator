<template>
  <header class="workspace-page-header">
    <div class="workspace-page-header__inner">
      <div class="workspace-page-header__content">
        <span class="workspace-page-header__accent" aria-hidden="true"></span>
        <div>
          <h1 class="workspace-page-header__title">{{ title }}</h1>
          <p class="workspace-page-header__subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <div v-if="$slots.actions" class="workspace-page-header__actions">
        <slot name="actions" />
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle: string
}>()
</script>

<style scoped lang="scss">
.workspace-page-header {
  position: relative;
  overflow: hidden;
  margin-bottom: 24px;
  padding: 28px 20px;
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(135deg, rgba(224, 236, 229, 0.72), rgba(247, 245, 238, 0.58));
  isolation: isolate;
}

.workspace-page-header__inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  max-width: 1400px;
  min-height: 108px;
  margin: 0 auto;
}

.workspace-page-header__content {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.workspace-page-header__accent {
  width: 4px;
  height: 52px;
  flex: 0 0 4px;
  border-radius: var(--radius-full);
  background: var(--mountain-green);
  box-shadow: 0 6px 16px rgba(69, 111, 100, 0.22);
}

.workspace-page-header__title {
  margin: 0;
  color: var(--ink-deep);
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 600;
  line-height: 1.2;
}

.workspace-page-header__subtitle {
  margin: 8px 0 0;
  color: var(--ink-muted);
  font-size: 14px;
  line-height: 1.5;
}

.workspace-page-header__actions {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
}

:deep(.workspace-page-header__actions .ant-btn) {
  min-height: 40px;
  border-radius: var(--radius-full);
}

@media (max-width: 768px) {
  .workspace-page-header {
    margin-bottom: 18px;
    padding: 24px 16px;
  }

  .workspace-page-header__inner {
    align-items: flex-start;
    flex-direction: column;
    gap: 20px;
    min-height: 0;
  }

  .workspace-page-header__title {
    font-size: 28px;
  }

  .workspace-page-header__actions {
    width: 100%;
  }

  :deep(.workspace-page-header__actions .ant-btn) {
    width: 100%;
  }
}
</style>
