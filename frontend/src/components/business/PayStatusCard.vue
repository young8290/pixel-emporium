<script setup>
import successImg from '@/assets/pixel/illustrations/result-success.png'
import failImg from '@/assets/pixel/illustrations/result-fail.png'
defineProps({success:Boolean,order:{type:Object,default:()=>({})},title:{type:String,default:''}})
</script>
<template><section :class="['pay-status',{success}]"><img :src="success?successImg:failImg" :alt="success?'支付成功':'支付未完成'" data-pixel><h1>{{ title||(success?'好物已入袋！':'支付还没完成') }}</h1><p>订单号：{{ order.orderNo||'—' }}</p><strong v-if="order.amount!=null">¥{{ Number(order.amount).toFixed(2) }}</strong><div><slot /></div></section></template>
<style scoped>.pay-status{display:grid;justify-items:center;gap:12px;max-width:620px;margin:40px auto;padding:28px;border:4px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel-lg);text-align:center}.pay-status img{width:min(280px,80%)}.pay-status.success img{animation:pop var(--dur-3) var(--ease-bounce)}.pay-status h1{font-family:var(--font-pixel);font-size:28px;color:var(--c-wood-dark)}.pay-status strong{color:var(--c-amber);font-family:var(--font-pixel);font-size:32px}.pay-status>div{display:flex;gap:12px}@keyframes pop{from{opacity:0;transform:scale(.9)}}@media(prefers-reduced-motion:reduce){.pay-status.success img{animation:none}}</style>
