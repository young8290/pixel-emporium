<script setup>
import { computed, useSlots } from 'vue'
import placeholder from '@/assets/pixel/products/product-placeholder.png'
defineProps({items:{type:Array,default:()=>[]}})
const slots=useSlots()
const hasActions=computed(()=>Boolean(slots.actions))
</script>
<template><div class="order-items"><article v-for="item in items" :key="item.id||item.productSkuId" :class="{'has-actions':hasActions}"><img :src="item.image||item.productImage||placeholder" :alt="item.productTitle" data-pixel><div><strong>{{ item.productTitle }}</strong><p>{{ item.skuTitle }}</p></div><span>¥{{ Number(item.price||0).toFixed(2) }} × {{ item.quantity }}</span><b>¥{{ (Number(item.price||0)*Number(item.quantity||0)).toFixed(2) }}</b><div v-if="hasActions" class="item-actions"><slot name="actions" :item="item"/></div></article></div></template>
<style scoped>.order-items{display:grid}.order-items article{display:grid;grid-template-columns:70px minmax(120px,1fr) 150px 90px;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid var(--c-line)}.order-items article.has-actions{grid-template-columns:70px minmax(120px,1fr) 130px 80px minmax(150px,auto)}.order-items img{width:64px;height:64px;border:2px solid var(--c-wood);object-fit:cover}.order-items p,.order-items span{color:var(--c-ink-2)}.order-items b{color:var(--c-amber);text-align:right}.item-actions{min-width:0}@media(max-width:760px){.order-items article,.order-items article.has-actions{grid-template-columns:60px 1fr auto}.order-items article>span{grid-column:2}.order-items article>b{grid-column:3;grid-row:1/3}.item-actions{grid-column:2/4}}
</style>
