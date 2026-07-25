<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import CartItem from '@/components/business/CartItem.vue'
import SummaryBar from '@/components/business/SummaryBar.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import empty from '@/assets/pixel/empty-states/empty-cart.png'
import network from '@/assets/pixel/illustrations/error-network.png'
const router=useRouter(),cart=useCartStore(),failed=ref(false),busy=ref(null)
const valid=computed(()=>cart.items.filter(i=>!i.invalid)),all=computed(()=>valid.value.length>0&&valid.value.every(i=>i.checked))
async function load(){failed.value=false;try{await cart.fetch()}catch{failed.value=true}}
async function update(item,patch){busy.value=item.id;try{await cart.update({id:item.id,quantity:item.quantity,...patch})}finally{busy.value=null}}
async function toggleAll(value){await Promise.all(valid.value.filter(i=>i.checked!==value).map(i=>cart.update({id:i.id,quantity:i.quantity,checked:value})));await cart.fetch()}
function checkout(){router.push({path:'/checkout',query:{source:'cart',cartItemIds:cart.checkedItems.map(i=>i.id).join(',')}})}
onMounted(load)
</script>
<template><div class="cart-page"><header class="page-heading"><small>YOUR BASKET</small><h1>购物篮</h1></header><PixelSkeleton v-if="cart.loading&&!cart.items.length" :rows="5"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><PixelEmptyState v-else-if="!cart.items.length" :image="empty" title="篮子还是空的" desc="去集市挑几件新鲜好物吧。" cta="去逛逛" @cta-click="router.push('/products')"/><template v-else><section class="cart-list"><CartItem v-for="item in cart.items" :key="item.id" :item="item" :busy="busy===item.id" @update="update(item,{quantity:$event})" @check="update(item,{checked:$event})" @remove="cart.remove(item.id)"/></section><SummaryBar :total="cart.checkedAmount" :count="cart.checkedQuantity" :checked-all="all" @toggle-all="toggleAll" @checkout="checkout"/></template></div></template>
<style scoped>.cart-page{padding-top:var(--sp-4)}.page-heading{margin-bottom:var(--sp-4)}.page-heading small{color:var(--c-moss-dark)}.page-heading h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.cart-list{overflow:hidden;border:3px solid var(--c-wood-dark);box-shadow:var(--shadow-pixel)}</style>
