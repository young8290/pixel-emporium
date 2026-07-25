<script setup>
/**
 * PixelTextarea —— 薄封装 el-input(type=textarea) + 像素皮肤
 * 木边聚焦 amber；error 态 brick 边 + 折叠错误文案。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值 @type {string} */
  modelValue: { type: String, default: '' },
  /** 占位符 @type {string} */
  placeholder: { type: String, default: '' },
  /** 行数 @type {number} */
  rows: { type: Number, default: 3 },
  /** 最大长度 @type {number} */
  maxlength: { type: [Number, String], default: undefined },
  /** 是否显示字数 @type {boolean} */
  showWordLimit: { type: Boolean, default: false },
  /** 自适应高度 @type {boolean|object} */
  autosize: { type: [Boolean, Object], default: false },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false },
  /** 错误文案 @type {string} */
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'input'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<template>
  <div class="pixel-textarea" :class="{ 'is-error': !!error }">
    <el-input
      v-model="inner"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :show-word-limit="showWordLimit"
      :autosize="autosize"
      :disabled="disabled"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
      @input="$emit('input', $event)"
    />
    <transition name="pixel-err">
      <p v-if="error" class="pixel-textarea__error">{{ error }}</p>
    </transition>
  </div>
</template>

<style scoped>
.pixel-textarea { width: 100%; }
.pixel-textarea :deep(.el-textarea__inner) {
  background: var(--c-surface);
  border-radius: var(--radius);
  box-shadow: 0 0 0 2px var(--c-wood) inset;
  transition: box-shadow var(--dur-1) var(--ease-std);
  font-family: var(--font-sans);
}
.pixel-textarea :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset;
}
.pixel-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset, 0 0 0 4px var(--c-amber);
}
.pixel-textarea :deep(.el-textarea.is-disabled .el-textarea__inner) {
  background: var(--c-disabled);
  box-shadow: 0 0 0 2px var(--c-line) inset;
}
.pixel-textarea.is-error :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 2px var(--c-brick) inset;
}
.pixel-textarea.is-error :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px var(--c-brick) inset, 0 0 0 4px var(--c-amber);
}
.pixel-textarea__error {
  margin: var(--sp-1) 0 0;
  color: var(--c-brick);
  font-size: var(--fs-sm);
  line-height: 1.4;
}
.pixel-err-enter-active,
.pixel-err-leave-active {
  transition: max-height var(--dur-2) var(--ease-std), opacity var(--dur-2) var(--ease-std);
  overflow: hidden;
  max-height: 40px;
}
.pixel-err-enter-from,
.pixel-err-leave-to { max-height: 0; opacity: 0; }

@media (prefers-reduced-motion: reduce) {
  .pixel-err-enter-active,
  .pixel-err-leave-active { transition: none; }
}
</style>
