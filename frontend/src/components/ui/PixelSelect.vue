<script setup>
/**
 * PixelSelect —— 薄封装 el-select + 像素皮肤
 * 木边聚焦 amber；options 数组配置或默认插槽自定义 option。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值 @type {string|number|Array} */
  modelValue: { type: [String, Number, Array], default: '' },
  /** 选项列表 @type {Array<{label:string,value:any,disabled?:boolean}>} */
  options: { type: Array, default: () => [] },
  /** 占位符 @type {string} */
  placeholder: { type: String, default: '请选择' },
  /** 多选 @type {boolean} */
  multiple: { type: Boolean, default: false },
  /** 可清空 @type {boolean} */
  clearable: { type: Boolean, default: false },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'change', 'clear'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<template>
  <div class="pixel-select">
    <el-select
      v-model="inner"
      :placeholder="placeholder"
      :multiple="multiple"
      :clearable="clearable"
      :disabled="disabled"
      popper-class="pixel-select-popper"
      @change="$emit('change', $event)"
      @clear="$emit('clear')"
    >
      <slot>
        <el-option
          v-for="opt in options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
          :disabled="opt.disabled"
        />
      </slot>
    </el-select>
  </div>
</template>

<style scoped>
.pixel-select { width: 100%; }
.pixel-select :deep(.el-select__wrapper) {
  background: var(--c-surface);
  border-radius: var(--radius);
  box-shadow: 0 0 0 2px var(--c-wood) inset;
  transition: box-shadow var(--dur-1) var(--ease-std);
  min-height: var(--h-input);
}
.pixel-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset;
}
.pixel-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset, 0 0 0 4px var(--c-amber);
}
.pixel-select :deep(.el-select__wrapper.is-disabled) {
  background: var(--c-disabled);
  box-shadow: 0 0 0 2px var(--c-line) inset;
}
</style>

<style>
/* 下拉浮层皮肤（非 scoped，浮层挂在 body） */
.pixel-select-popper.el-popper {
  background: var(--c-surface);
  border: 2px solid var(--c-wood);
  border-radius: var(--radius);
  box-shadow: var(--shadow-pixel);
}
.pixel-select-popper .el-select-dropdown__item.is-selected {
  color: var(--c-moss-dark);
  font-weight: var(--fw-bold);
}
.pixel-select-popper .el-select-dropdown__item.is-hovering {
  background: var(--c-surface-2);
}
</style>
