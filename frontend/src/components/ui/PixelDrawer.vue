<script setup>
defineProps({ modelValue: Boolean, title: { type: String, default: '' }, size: { type: String, default: '82%' }, direction: { type: String, default: 'ltr' } })
defineEmits(['update:modelValue'])
</script>
<template>
  <el-drawer :model-value="modelValue" :title="title" :size="size" :direction="direction"
    class="pixel-drawer" @update:model-value="$emit('update:modelValue', $event)"><slot /></el-drawer>
</template>
<style>
.pixel-drawer{background:var(--c-surface)!important;border-right:4px solid var(--c-wood-dark)}.pixel-drawer .el-drawer__title{font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}
</style>
