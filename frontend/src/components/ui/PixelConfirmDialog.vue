<script setup>
import PixelDialog from './PixelDialog.vue'
import PixelButton from './PixelButton.vue'
defineProps({ modelValue: Boolean, title: { type: String, default: '请确认' }, content: { type: String, default: '' }, danger: Boolean, loading: Boolean })
defineEmits(['update:modelValue', 'confirm', 'cancel'])
</script>
<template>
  <PixelDialog :model-value="modelValue" :title="title"
    @update:model-value="$emit('update:modelValue', $event)" @cancel="$emit('cancel')">
    <p class="confirm-copy"><slot>{{ content }}</slot></p>
    <template #footer>
      <PixelButton type="ghost" @click="$emit('cancel')">返回</PixelButton>
      <PixelButton :type="danger ? 'brick' : 'moss'" :loading="loading" @click="$emit('confirm')">确认</PixelButton>
    </template>
  </PixelDialog>
</template>
<style scoped>.confirm-copy{line-height:1.8;color:var(--c-ink)}</style>
