<script setup>
/**
 * PixelTag —— ★自研 标签（订单状态色映射）
 * 与 Badge 区别：Tag 用于内容标注/筛选（可关闭），弱边框浅底；Badge 用于强调计数/状态点。
 * plain 模式浅底描边，实心模式填充状态色。可关闭 closable。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 标签文字 @type {string} */
  text: { type: String, default: '' },
  /**
   * 状态色（订单/通用语义）
   * @type {'moss'|'amber'|'pumpkin'|'brick'|'wood'|'sky'|'info'
   *        |'pending'|'paid'|'shipped'|'completed'|'cancelled'|'refund'}
   */
  color: { type: String, default: 'wood' },
  /** 尺寸 @type {'small'|'default'} */
  size: { type: String, default: 'default' },
  /** 浅底描边风格（false 为实心填充） @type {boolean} */
  plain: { type: Boolean, default: true },
  /** 可关闭 @type {boolean} */
  closable: { type: Boolean, default: false }
})

const emit = defineEmits(['close'])

const STATUS_MAP = {
  moss: 'moss', amber: 'amber', pumpkin: 'pumpkin', brick: 'brick',
  wood: 'wood', sky: 'sky', info: 'ink-2',
  pending: 'amber', paid: 'moss', shipped: 'sky',
  completed: 'moss', cancelled: 'ink-2', refund: 'brick'
}

const colorVar = computed(() => `var(--c-${STATUS_MAP[props.color] || 'wood'})`)
</script>

<template>
  <span
    class="pixel-tag"
    :class="[`pixel-tag--${size}`, plain ? 'is-plain' : 'is-solid']"
    :style="{ '--tag-c': colorVar }"
  >
    <slot>{{ text }}</slot>
    <button
      v-if="closable"
      class="pixel-tag__close"
      type="button"
      aria-label="移除标签"
      @click="emit('close', $event)"
    >×</button>
  </span>
</template>

<style scoped>
.pixel-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-1);
  padding: 2px var(--sp-2);
  border: 2px solid var(--tag-c);
  border-radius: var(--radius-sm);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  line-height: 1.4;
  white-space: nowrap;
}
.pixel-tag--small { padding: 1px var(--sp-1); font-size: 10px; }

/* 浅底描边：底用面色，字/边用状态色 */
.pixel-tag.is-plain {
  background: var(--c-surface);
  color: var(--tag-c);
}
/* 实心填充 */
.pixel-tag.is-solid {
  background: var(--tag-c);
  color: var(--c-surface);
  border-color: var(--c-wood-dark);
}

.pixel-tag__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  padding: 0;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: transform var(--dur-1) var(--ease-std);
}
.pixel-tag__close:hover { transform: scale(1.2); }
.pixel-tag__close:focus-visible {
  outline: 2px solid var(--c-amber);
  outline-offset: 1px;
}

@media (prefers-reduced-motion: reduce) {
  .pixel-tag__close { transition: none; }
  .pixel-tag__close:hover { transform: none; }
}
</style>
