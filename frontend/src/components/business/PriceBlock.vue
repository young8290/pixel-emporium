<script setup>
import { computed } from 'vue'

const props = defineProps({price:{type:[Number,String],default:0},original:{type:[Number,String],default:null}})
const hasDiscount = computed(() => {
  const price = Number(props.price)
  const original = Number(props.original)
  return Number.isFinite(price) && Number.isFinite(original) && original > price
})
const discountText = computed(() => `${(Number(props.price) / Number(props.original) * 10).toFixed(1).replace('.0', '')}折`)
</script>
<template><div class="price-block"><strong><small>¥</small>{{ Number(price||0).toFixed(2) }}</strong><del v-if="hasDiscount">¥{{ Number(original).toFixed(2) }}</del><span v-if="hasDiscount" class="discount-label" data-test="discount-badge">{{ discountText }}</span></div></template>
<style scoped>.price-block{display:flex;align-items:end;gap:10px}.price-block strong{color:var(--c-amber);font-family:var(--font-pixel);font-size:36px;text-shadow:2px 2px 0 var(--c-wood-dark),-1px -1px 0 var(--c-wood-dark)}.price-block small{font-size:18px}.price-block del{color:var(--c-ink-2)}.discount-label{padding:3px 5px;border:2px solid var(--c-brick);color:var(--c-brick);font-size:12px;line-height:1;background:var(--c-surface)}</style>
