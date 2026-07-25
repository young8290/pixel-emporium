<script setup>
defineProps({status:{type:String,default:'PENDING_PAYMENT'}})
const steps=[['PENDING_PAYMENT','提交订单'],['PAID','付款成功'],['SHIPPED','商家发货'],['COMPLETED','交易完成']]
const rank={PENDING_PAYMENT:0,PAID:1,SHIPPED:2,COMPLETED:3,CANCELED:0}
</script>
<template><div class="timeline" :class="{canceled:status==='CANCELED'}"><div v-for="([value,label],i) in steps" :key="value" :class="{done:i<=rank[status]}"><i>{{ i+1 }}</i><span>{{ label }}</span></div><p v-if="status==='CANCELED'">订单已取消</p></div></template>
<style scoped>.timeline{position:relative;display:grid;grid-template-columns:repeat(4,1fr);padding:18px}.timeline::before{content:"";position:absolute;left:12%;right:12%;top:35px;height:4px;background:var(--c-line)}.timeline>div{position:relative;z-index:1;display:grid;justify-items:center;gap:8px;color:var(--c-ink-2)}.timeline i{display:grid;place-items:center;width:36px;height:36px;border:3px solid var(--c-wood);background:var(--c-surface);font-style:normal}.timeline .done i{background:var(--c-moss);color:var(--c-surface)}.timeline .done{color:var(--c-moss-dark)}.timeline p{position:absolute;inset:0;display:grid;place-items:center;background:color-mix(in srgb,var(--c-surface) 88%,transparent);color:var(--c-brick);font-family:var(--font-pixel);font-size:24px}@media(max-width:520px){.timeline span{font-size:11px}}</style>
