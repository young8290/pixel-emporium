<script setup>
import PixelTag from '@/components/ui/PixelTag.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import OrderItemList from './OrderItemList.vue'
defineProps({order:{type:Object,required:true}})
defineEmits(['pay','cancel','confirm','detail'])
const labels={PENDING_PAYMENT:'待付款',PAID:'待发货',SHIPPED:'待收货',COMPLETED:'已完成',CANCELED:'已取消'}
const colors={PENDING_PAYMENT:'amber',PAID:'moss',SHIPPED:'wood',COMPLETED:'moss',CANCELED:'info'}
</script>
<template><article class="order-card"><header><span>订单 {{ order.orderNo }}</span><time>{{ order.createdAt }}</time><PixelTag :text="labels[order.status]||order.status" :color="colors[order.status]||'wood'" /></header><OrderItemList :items="order.items||[]" /><footer><strong>实付 ¥{{ Number(order.payAmount??order.totalAmount??0).toFixed(2) }}</strong><PixelButton type="ghost" size="small" @click="$emit('detail')">查看详情</PixelButton><PixelButton v-if="order.status==='PENDING_PAYMENT'" type="brick" size="small" @click="$emit('cancel')">取消</PixelButton><PixelButton v-if="order.status==='PENDING_PAYMENT'" size="small" @click="$emit('pay')">付款</PixelButton><PixelButton v-if="order.status==='SHIPPED'" size="small" @click="$emit('confirm')">确认收货</PixelButton></footer></article></template>
<style scoped>.order-card{border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.order-card header,.order-card footer{display:flex;align-items:center;gap:12px;padding:12px 16px;background:var(--c-surface-2)}.order-card header time{margin-left:auto;color:var(--c-ink-2);font-size:12px}.order-card :deep(.order-items){padding:0 16px}.order-card footer{justify-content:flex-end}.order-card footer strong{margin-right:auto;color:var(--c-amber)}@media(max-width:600px){.order-card header{flex-wrap:wrap}.order-card header time{order:3;width:100%}.order-card footer{flex-wrap:wrap}.order-card footer strong{width:100%}}</style>
