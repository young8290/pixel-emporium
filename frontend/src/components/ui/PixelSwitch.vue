<script setup>
/**
 * PixelSwitch —— 薄封装 el-switch + 像素皮肤
 * 滑块 translateX 切换 + --ease-bounce 小回弹；开启 moss 底。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值 @type {boolean|string|number} */
  modelValue: { type: [Boolean, String, Number], default: false },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false },
  /** 开启时的值 @type {boolean|string|number} */
  activeValue: { type: [Boolean, String, Number], default: true },
  /** 关闭时的值 @type {boolean|string|number} */
  inactiveValue: { type: [Boolean, String, Number], default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<template>
  <el-switch
    v-model="inner"
    class="pixel-switch"
    :disabled="disabled"
    :active-value="activeValue"
    :inactive-value="inactiveValue"
    @change="$emit('change', $event)"
  />
</template>

<style scoped>
.pixel-switch {
  --el-switch-on-color: var(--c-moss);
  --el-switch-off-color: var(--c-surface-2);
}
.pixel-switch :deep(.el-switch__core) {
  border: 2px solid var(--c-wood);
  border-radius: var(--radius);
  background: var(--c-surface-2);
}
.pixel-switch :deep(.el-switch__core .el-switch__action) {
  background: var(--c-surface);
  border-radius: var(--radius-sm);
  transition: transform var(--dur-1) var(--ease-bounce),
    left var(--dur-1) var(--ease-bounce);
}
.pixel-switch.is-checked :deep(.el-switch__core) {
  border-color: var(--c-wood-dark);
  background: var(--c-moss);
}
.pixel-switch :deep(.el-switch.is-focus .el-switch__core) {
  outline: 2px solid var(--c-amber);
  outline-offset: 1px;
}
.pixel-switch :deep(.el-switch.is-disabled .el-switch__core) {
  background: var(--c-disabled);
  border-color: var(--c-line);
}

@media (prefers-reduced-motion: reduce) {
  .pixel-switch :deep(.el-switch__core .el-switch__action) { transition: none; }
}
</style>
