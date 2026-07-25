<script setup>
/**
 * PixelButton —— 薄封装 el-button + 星露谷像素皮肤
 * 硬像素投影、hover 抬升、press 下沉、focus amber 描边、loading 像素 spinner。
 * 动效遵循 docs/design/10 §6.2：只动 transform/opacity，reduce 下即时切换。
 */
import PixelLoading from './PixelLoading.vue'

const props = defineProps({
  /** 按钮配色 @type {'moss'|'amber'|'wood'|'brick'|'ghost'|'pumpkin'} */
  type: { type: String, default: 'moss' },
  /** 尺寸 @type {'default'|'small'} */
  size: { type: String, default: 'default' },
  /** 加载态：文字淡出替换像素 spinner，宽度锁定防跳动 @type {boolean} */
  loading: { type: Boolean, default: false },
  /** 禁用 @type {boolean} */
  disabled: { type: Boolean, default: false },
  /** 是否块级铺满 @type {boolean} */
  block: { type: Boolean, default: false },
  /** 原生 button type @type {'button'|'submit'|'reset'} */
  nativeType: { type: String, default: 'button' }
})

const emit = defineEmits(['click'])

function onClick(e) {
  if (props.disabled || props.loading) return
  emit('click', e)
}
</script>

<template>
  <button
    class="pixel-btn"
    :class="[
      `pixel-btn--${type}`,
      `pixel-btn--${size}`,
      { 'is-loading': loading, 'is-disabled': disabled, 'is-block': block }
    ]"
    :type="nativeType"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : 'false'"
    @click="onClick"
    @keydown.enter.prevent="onClick"
  >
    <span v-if="loading" class="pixel-btn__spinner">
      <PixelLoading :size="size === 'small' ? 14 : 18" />
    </span>
    <span class="pixel-btn__label" :class="{ 'is-hidden': loading }">
      <span v-if="$slots.icon" class="pixel-btn__icon"><slot name="icon" /></span>
      <slot />
    </span>
  </button>
</template>

<style scoped>
.pixel-btn {
  --btn-bg: var(--c-moss);
  --btn-fg: var(--c-surface);
  --btn-border: var(--c-wood-dark);
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--sp-2);
  height: var(--h-btn);
  min-width: 44px;
  padding: 0 var(--sp-4);
  border: 2px solid var(--btn-border);
  border-radius: var(--radius);
  background: var(--btn-bg);
  color: var(--btn-fg);
  font-family: var(--font-pixel);
  font-size: 16px;
  font-weight: var(--fw-bold);
  line-height: 1;
  cursor: pointer;
  box-shadow: var(--shadow-pixel);
  transition: transform var(--dur-1) var(--ease-std),
    box-shadow var(--dur-1) var(--ease-std),
    background-color var(--dur-1) var(--ease-std);
  user-select: none;
}

.pixel-btn--small {
  height: var(--h-btn-sm);
  padding: 0 var(--sp-3);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
}
.pixel-btn.is-block { width: 100%; }

/* 配色变体（全部走 Token；hover 用 moss-dark 或亮度滤镜加深，避免硬编码新色值） */
.pixel-btn--moss { --btn-bg: var(--c-moss); --btn-fg: var(--c-surface); }
.pixel-btn--moss:not(.is-disabled):not(.is-loading):hover { --btn-bg: var(--c-moss-dark); }
.pixel-btn--amber { --btn-bg: var(--c-amber); --btn-fg: var(--c-ink); }
.pixel-btn--wood { --btn-bg: var(--c-wood); --btn-fg: var(--c-surface); }
.pixel-btn--wood:not(.is-disabled):not(.is-loading):hover { --btn-bg: var(--c-wood-dark); }
.pixel-btn--brick { --btn-bg: var(--c-brick); --btn-fg: var(--c-surface); }
.pixel-btn--pumpkin { --btn-bg: var(--c-pumpkin); --btn-fg: var(--c-surface); }
.pixel-btn--ghost {
  --btn-bg: transparent;
  --btn-fg: var(--c-ink);
  --btn-border: var(--c-wood);
}
.pixel-btn--ghost:not(.is-disabled):not(.is-loading):hover { --btn-bg: var(--c-surface-2); }

/* hover 抬升 + 硬投影下移一档；amber/brick/pumpkin 无专属深色 Token，用亮度滤镜统一加深 */
.pixel-btn:not(.is-disabled):not(.is-loading):hover {
  background: var(--btn-bg);
  transform: translateY(calc(-1 * var(--rise)));
  box-shadow: var(--shadow-pixel-lg);
}
.pixel-btn--amber:not(.is-disabled):not(.is-loading):hover,
.pixel-btn--brick:not(.is-disabled):not(.is-loading):hover,
.pixel-btn--pumpkin:not(.is-disabled):not(.is-loading):hover {
  filter: brightness(0.92);
}

/* press 下沉 + 投影收起 */
.pixel-btn:not(.is-disabled):not(.is-loading):active {
  transform: translateY(var(--press));
  box-shadow: none;
}

/* focus amber 描边（键盘可达） */
.pixel-btn:focus-visible {
  outline: 2px solid var(--c-amber);
  outline-offset: 2px;
}

/* disabled：无过渡 */
.pixel-btn.is-disabled,
.pixel-btn:disabled {
  background: var(--c-disabled);
  color: var(--c-ink-2);
  border-color: var(--c-line);
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
  transition: none;
}

/* loading：文字淡出，spinner 覆盖，宽度锁定 */
.pixel-btn.is-loading { cursor: progress; }
.pixel-btn__label {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  transition: opacity var(--dur-1) var(--ease-std);
}
.pixel-btn__label.is-hidden { opacity: 0; }
.pixel-btn__icon { display: inline-flex; width: 1em; height: 1em; }
.pixel-btn__spinner {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

@media (prefers-reduced-motion: reduce) {
  .pixel-btn { transition: none; }
  .pixel-btn:not(.is-disabled):not(.is-loading):hover,
  .pixel-btn:not(.is-disabled):not(.is-loading):active { transform: none; }
}
</style>
