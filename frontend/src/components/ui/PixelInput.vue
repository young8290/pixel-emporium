<script setup>
/**
 * PixelInput —— 薄封装 el-input + 像素皮肤
 * 木边聚焦 amber 描边；error 态 brick 边框 + 下方错误文案折叠展开 + 抖动一次。
 * 动效遵循 docs/design/10 §6.2，reduce 下不抖、即时切换。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 绑定值 @type {string|number} */
  modelValue: { type: [String, Number], default: '' },
  /** 占位符 @type {string} */
  placeholder: { type: String, default: '' },
  /** 原生 type @type {string} */
  type: { type: String, default: 'text' },
  /** 最大长度 @type {number} */
  maxlength: { type: [Number, String], default: undefined },
  /** 是否显示字数 @type {boolean} */
  showWordLimit: { type: Boolean, default: false },
  /** 是否可清空 @type {boolean} */
  clearable: { type: Boolean, default: false },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false },
  /** 错误文案（非空则进入 error 态） @type {string} */
  error: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'input', 'clear'])

const inner = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
</script>

<template>
  <div class="pixel-input" :class="{ 'is-error': !!error }">
    <el-input
      v-model="inner"
      :type="type"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :show-word-limit="showWordLimit"
      :clearable="clearable"
      :disabled="disabled"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
      @input="$emit('input', $event)"
      @clear="$emit('clear')"
    >
      <template v-if="$slots.prefix" #prefix><slot name="prefix" /></template>
      <template v-if="$slots.suffix" #suffix><slot name="suffix" /></template>
      <template v-if="$slots.prepend" #prepend><slot name="prepend" /></template>
      <template v-if="$slots.append" #append><slot name="append" /></template>
    </el-input>
    <transition name="pixel-err">
      <p v-if="error" class="pixel-input__error">{{ error }}</p>
    </transition>
  </div>
</template>

<style scoped>
.pixel-input {
  width: 100%;
}
.pixel-input :deep(.el-input__wrapper) {
  background: var(--c-surface);
  border-radius: var(--radius);
  box-shadow: 0 0 0 2px var(--c-wood) inset;
  transition: box-shadow var(--dur-1) var(--ease-std);
}
.pixel-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset;
}
.pixel-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--c-wood-dark) inset, 0 0 0 4px var(--c-amber);
}
.pixel-input :deep(.el-input.is-disabled .el-input__wrapper) {
  background: var(--c-disabled);
  box-shadow: 0 0 0 2px var(--c-line) inset;
}

/* error 态：brick 边 + 抖动一次 */
.pixel-input.is-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 2px var(--c-brick) inset;
  animation: pixel-shake var(--dur-2) var(--ease-std) 1;
}
.pixel-input.is-error :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--c-brick) inset, 0 0 0 4px var(--c-amber);
}

.pixel-input__error {
  margin: var(--sp-1) 0 0;
  color: var(--c-brick);
  font-size: var(--fs-sm);
  line-height: 1.4;
}

/* error 文案高度折叠展开（max-height 特例，见 §6.2） */
.pixel-err-enter-active,
.pixel-err-leave-active {
  transition: max-height var(--dur-2) var(--ease-std), opacity var(--dur-2) var(--ease-std);
  overflow: hidden;
  max-height: 40px;
}
.pixel-err-enter-from,
.pixel-err-leave-to {
  max-height: 0;
  opacity: 0;
}

@keyframes pixel-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(calc(-1 * var(--press))); }
  75% { transform: translateX(var(--press)); }
}

@media (prefers-reduced-motion: reduce) {
  .pixel-input.is-error :deep(.el-input__wrapper) { animation: none; }
  .pixel-err-enter-active,
  .pixel-err-leave-active { transition: none; }
}
</style>
