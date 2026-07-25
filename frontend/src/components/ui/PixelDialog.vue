<script setup>
import { computed } from 'vue'

const props = defineProps({ modelValue: Boolean, title: { type: String, default: '' }, width: { type: String, default: '520px' } })
defineEmits(['update:modelValue', 'confirm', 'cancel'])
const responsiveWidth = computed(() => `min(${props.width}, calc(100vw - 24px))`)
</script>
<template>
  <el-dialog :model-value="modelValue" :title="title" :width="responsiveWidth" class="pixel-dialog"
    @update:model-value="$emit('update:modelValue', $event)" @close="$emit('cancel')">
    <slot />
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </el-dialog>
</template>
<style>
.pixel-dialog{border:4px solid var(--c-wood-dark)!important;background:var(--c-surface)!important;box-shadow:4px 4px 0 var(--c-wood)!important}.pixel-dialog .el-dialog__title{font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}
</style>
