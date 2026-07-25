<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api/order'
import { useUserStore } from '@/stores/user'
import OrderCard from '@/components/business/OrderCard.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import network from '@/assets/pixel/illustrations/error-network.png'
import avatar from '@/assets/pixel/products/avatar-placeholder.png'
const router=useRouter(),user=useUserStore(),orders=ref([]),loading=ref(true),failed=ref(false)
const stats=computed(()=>[
  ['全部订单',orders.value.length],['待付款',orders.value.filter(o=>o.status==='PENDING_PAYMENT').length],
  ['待收货',orders.value.filter(o=>o.status==='SHIPPED').length],['已完成',orders.value.filter(o=>o.status==='COMPLETED').length]
])
async function load(){loading.value=true;failed.value=false;try{const data=await listOrders({page:1,size:50});orders.value=data?.records||data?.list||[]}catch{failed.value=true}finally{loading.value=false}}
onMounted(load)
</script>
<template><div class="overview"><section class="welcome"><img :src="avatar" alt="默认头像" data-pixel><div><small>WELCOME BACK</small><h1>欢迎回来，{{ user.email }}</h1><p>今天也来看看商城里有什么新品吧。</p></div></section><PixelSkeleton v-if="loading" :rows="5"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><template v-else><section class="stats"><button v-for="[label,value] in stats" :key="label" @click="router.push('/user/orders')"><b>{{ value }}</b><span>{{ label }}</span></button></section><h2>最近订单</h2><section class="recent"><OrderCard v-for="order in orders.slice(0,3)" :key="order.id" :order="order" @detail="router.push(`/user/orders/${order.id}`)" @pay="router.push({path:'/pay',query:{orderNo:order.orderNo}})"/></section></template></div></template>
<style scoped>.overview{display:grid;gap:var(--sp-4)}.welcome{display:flex;align-items:center;gap:18px;padding:var(--sp-4);border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.welcome img{width:72px}.welcome small{color:var(--c-moss-dark)}h1,h2{font-family:var(--font-pixel);color:var(--c-wood-dark)}h1{font-size:25px}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.stats button{display:grid;gap:4px;min-height:96px;border:3px solid var(--c-wood);background:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer}.stats b{font-family:var(--font-pixel);font-size:28px;color:var(--c-amber)}.recent{display:grid;gap:14px}@media(max-width:640px){.stats{grid-template-columns:repeat(2,1fr)}.welcome{align-items:flex-start}.welcome p{font-size:13px}}</style>
