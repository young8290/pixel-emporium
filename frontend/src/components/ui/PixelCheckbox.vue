<script setup>
/**
 * PixelCheckbox —— 薄封装 el-checkbox + 像素勾选皮肤
 * 勾选用 --ease-bounce 小回弹（像素勾「啪」一下）；focus amber 描边。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值：单个用 boolean，配合 el-checkbox-group 用 label 值 @type {boolean|string|number} */
  modelValue: { type: [Boolean, String, Number], default: false },
  /** group 场景下的选项值 @type {string|number|boolean} */
  label: { type: [String, Number, Boolean], default: undefined },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<template>
  <el-checkbox
    v-model="inner"
    class="pixel-checkbox"
    :label="label"
    :disabled="disabled"
    @change="$emit('change', $event)"
  >
    <slot />
  </el-checkbox>
</template>

<style scoped>
.pixel-checkbox :deep(.el-checkbox__inner) {
  background: var(--c-surface);
  border: 2px solid var(--c-wood);
  border-radius: var(--radius-sm);
  width: 18px;
  height: 18px;
  transition: transform var(--dur-1) var(--ease-bounce),
    background-color var(--dur-1) var(--ease-std);
}
.pixel-checkbox :deep(.el-checkbox__inner::after) {
  border-color: var(--c-surface);
  left: 5px;
  top: 1px;
  height: 8px;
  width: 4px;
}
.pixel-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: var(--c-moss);
  border-color: var(--c-wood-dark);
  transform: scale(1.06);
}
.pixel-checkbox :deep(.el-checkbox__input.is-focus .el-checkbox__inner) {
  outline: 2px solid var(--c-amber);
  outline-offset: 1px;
}
.pixel-checkbox :deep(.el-checkbox__input.is-disabled .el-checkbox__inner) {
  background: var(--c-disabled);
  border-color: var(--c-line);
}
.pixel-checkbox :deep(.el-checkbox__label) {
  color: var(--c-ink);
  font-family: var(--font-sans);
}

@media (prefers-reduced-motion: reduce) {
  .pixel-checkbox :deep(.el-checkbox__inner) { transition: none; }
  .pixel-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) { transform: none; }
}
</style>
