<script setup>
/**
 * PixelRadio —— 薄封装 el-radio + 像素皮肤
 * 选中 moss 填充 + 小回弹；focus amber 描边。可传 options 快速渲染一组。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值 @type {string|number|boolean} */
  modelValue: { type: [String, Number, Boolean], default: '' },
  /** 选项列表（传入则渲染一组 radio） @type {Array<{label:string,value:any,disabled?:boolean}>} */
  options: { type: Array, default: () => [] },
  /** 单个 radio 的值（不传 options 时用） @type {string|number|boolean} */
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
  <el-radio-group
    v-if="options.length"
    v-model="inner"
    class="pixel-radio"
    :disabled="disabled"
    @change="$emit('change', $event)"
  >
    <el-radio
      v-for="opt in options"
      :key="opt.value"
      :value="opt.value"
      :disabled="opt.disabled"
    >{{ opt.label }}</el-radio>
  </el-radio-group>
  <el-radio
    v-else
    v-model="inner"
    class="pixel-radio"
    :value="label"
    :disabled="disabled"
    @change="$emit('change', $event)"
  >
    <slot />
  </el-radio>
</template>

<style scoped>
.pixel-radio :deep(.el-radio__inner) {
  background: var(--c-surface);
  border: 2px solid var(--c-wood);
  width: 18px;
  height: 18px;
  transition: transform var(--dur-1) var(--ease-bounce),
    background-color var(--dur-1) var(--ease-std);
}
.pixel-radio :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: var(--c-moss);
  border-color: var(--c-wood-dark);
  transform: scale(1.06);
}
.pixel-radio :deep(.el-radio__input.is-checked .el-radio__inner::after) {
  background: var(--c-surface);
  width: 6px;
  height: 6px;
}
.pixel-radio :deep(.el-radio__input.is-focus .el-radio__inner) {
  outline: 2px solid var(--c-amber);
  outline-offset: 1px;
}
.pixel-radio :deep(.el-radio__input.is-disabled .el-radio__inner) {
  background: var(--c-disabled);
  border-color: var(--c-line);
}
.pixel-radio :deep(.el-radio__label) {
  color: var(--c-ink);
  font-family: var(--font-sans);
}

@media (prefers-reduced-motion: reduce) {
  .pixel-radio :deep(.el-radio__inner) { transition: none; }
  .pixel-radio :deep(.el-radio__input.is-checked .el-radio__inner) { transform: none; }
}
</style>
