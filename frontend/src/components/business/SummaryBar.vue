<script setup>
import PixelCheckbox from '@/components/ui/PixelCheckbox.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
defineProps({total:{type:[Number,String],default:0},count:{type:Number,default:0},checkedAll:Boolean,disabled:Boolean})
defineEmits(['checkout','toggleAll'])
</script>
<template><aside class="summary-bar"><PixelCheckbox :model-value="checkedAll" @update:model-value="$emit('toggleAll',$event)">全选</PixelCheckbox><span>已选 <b>{{ count }}</b> 件</span><strong><small>合计</small> ¥{{ Number(total||0).toFixed(2) }}</strong><PixelButton :disabled="disabled||!count" @click="$emit('checkout')">去结算</PixelButton></aside></template>
<style scoped>
.summary-bar{position:sticky;bottom:10px;z-index:12;display:flex;align-items:center;justify-content:flex-end;gap:var(--sp-5);padding:14px 18px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel-lg)}.summary-bar span{color:var(--c-ink-2)}.summary-bar b{color:var(--c-moss-dark)}.summary-bar strong{color:var(--c-amber);font-family:var(--font-pixel);font-size:24px}.summary-bar small{color:var(--c-ink-2);font:12px var(--font-sans)}@media(max-width:650px){.summary-bar{position:fixed;left:0;right:0;bottom:60px;gap:10px;padding:9px 12px}.summary-bar>span{display:none}.summary-bar strong{margin-left:auto;font-size:18px}}
</style>
