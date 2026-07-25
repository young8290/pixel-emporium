<script setup>
/**
 * PixelBadge —— ★自研 状态徽标 / 角标
 * 两种用法：
 *  1) 独立徽标：显示 text，按 status 取状态色（订单状态语义）。
 *  2) 角标：包裹 slot 内容，右上角浮一个数字/小圆点（dot）。
 * 纯 CSS + Token，无硬编码色。
 */
import { computed } from 'vue'

const props = defineProps({
  /** 徽标文字（独立徽标用） @type {string|number} */
  text: { type: [String, Number], default: '' },
  /**
   * 状态色映射（订单/通用语义）
   * @type {'moss'|'amber'|'pumpkin'|'brick'|'wood'|'sky'|'info'
   *        |'pending'|'paid'|'shipped'|'completed'|'cancelled'|'refund'}
   */
  status: { type: String, default: 'moss' },
  /** 尺寸 @type {'small'|'default'} */
  size: { type: String, default: 'default' },
  /** 角标模式：包裹 slot，右上角浮标 @type {boolean} */
  corner: { type: Boolean, default: false },
  /** 角标数值（corner 模式） @type {string|number} */
  value: { type: [String, Number], default: '' },
  /** 只显示小圆点（corner 模式，忽略 value） @type {boolean} */
  dot: { type: Boolean, default: false },
  /** 超过 max 显示 max+ @type {number} */
  max: { type: Number, default: 99 },
  /** 值为 0 时是否隐藏 @type {boolean} */
  hidden: { type: Boolean, default: false }
})

/** 订单状态 → 语义色 Token 名映射 */
const STATUS_MAP = {
  moss: 'moss', amber: 'amber', pumpkin: 'pumpkin', brick: 'brick',
  wood: 'wood', sky: 'sky', info: 'ink-2',
  pending: 'amber',    // 待付款
  paid: 'moss',        // 已付款
  shipped: 'sky',      // 已发货
  completed: 'moss',   // 已完成
  cancelled: 'ink-2',  // 已取消
  refund: 'brick'      // 退款/售后
}

const colorVar = computed(() => `var(--c-${STATUS_MAP[props.status] || 'moss'})`)

const displayValue = computed(() => {
  if (props.dot) return ''
  const n = Number(props.value)
  if (!Number.isNaN(n) && n > props.max) return `${props.max}+`
  return props.value
})

const showCorner = computed(() => {
  if (props.hidden) return false
  if (props.dot) return true
  return props.value !== '' && props.value !== 0 && props.value !== '0'
})
</script>

<template>
  <!-- 角标模式 -->
  <span v-if="corner" class="pixel-badge-wrap">
    <slot />
    <sup
      v-if="showCorner"
      class="pixel-badge-corner"
      :class="{ 'is-dot': dot }"
      :style="{ '--badge-c': colorVar }"
    >{{ displayValue }}</sup>
  </span>

  <!-- 独立徽标 -->
  <span
    v-else
    class="pixel-badge"
    :class="[`pixel-badge--${size}`]"
    :style="{ '--badge-c': colorVar }"
  >
    <slot>{{ text }}</slot>
  </span>
</template>

<style scoped>
.pixel-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 var(--sp-2);
  min-height: 20px;
  border: 2px solid var(--c-wood-dark);
  border-radius: var(--radius-sm);
  background: var(--badge-c);
  color: var(--c-surface);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: var(--fw-bold);
  line-height: 1;
  white-space: nowrap;
  box-shadow: var(--shadow-pixel);
}
.pixel-badge--small { min-height: 16px; padding: 0 var(--sp-1); font-size: 10px; }

/* 角标 */
.pixel-badge-wrap {
  position: relative;
  display: inline-flex;
}
.pixel-badge-corner {
  position: absolute;
  top: 0;
  right: 0;
  transform: translate(50%, -50%);
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border: 2px solid var(--c-surface);
  border-radius: 9px;
  background: var(--badge-c);
  color: var(--c-surface);
  font-family: var(--font-sans);
  font-size: 10px;
  font-weight: var(--fw-bold);
  line-height: 14px;
  text-align: center;
  box-shadow: var(--shadow-pixel);
}
.pixel-badge-corner.is-dot {
  min-width: 10px;
  width: 10px;
  height: 10px;
  padding: 0;
  border-radius: 50%;
}
</style>
