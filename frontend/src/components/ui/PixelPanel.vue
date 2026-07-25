<script setup>
defineProps({ title: { type: String, default: '' } })
</script>
<template>
  <section class="panel pixel-nine-card">
    <div v-if="title || $slots.title" class="panel__sign pixel-nine-sign"><slot name="title">{{ title }}</slot></div>
    <div class="panel__body"><slot /></div>
  </section>
</template>
<style scoped>
.panel{position:relative;margin-top:24px;background:var(--c-surface);box-shadow:var(--shadow-pixel)}.panel__sign{display:inline-flex;align-items:center;min-height:44px;margin:-26px 0 8px 16px;padding:8px 22px;color:var(--c-surface);font-family:var(--font-pixel);font-size:24px}.panel__body{padding:var(--sp-4)}
</style>
