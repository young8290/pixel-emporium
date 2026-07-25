<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { cancelOrder, confirmOrder, listOrders } from '@/api/order'
import OrderStatusTabs from '@/components/business/OrderStatusTabs.vue'
import OrderCard from '@/components/business/OrderCard.vue'
import PixelConfirmDialog from '@/components/ui/PixelConfirmDialog.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelPagination from '@/components/ui/PixelPagination.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import empty from '@/assets/pixel/empty-states/empty-order.png'
import network from '@/assets/pixel/illustrations/error-network.png'

const router = useRouter()
const status = ref('')
const orders = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const failed = ref(false)
const confirm = ref({ open: false, type: '', order: null, loading: false })

async function load() {
  loading.value = true
  failed.value = false
  try {
    const data = await listOrders({ page: page.value, size: 10, status: status.value || undefined })
    orders.value = data?.records || data?.list || []
    total.value = Number(data?.total || orders.value.length)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
function ask(type, order){confirm.value={open:true,type,order,loading:false}}
async function execute(){
  confirm.value.loading=true
  try{
    if(confirm.value.type==='cancel')await cancelOrder(confirm.value.order.id)
    else await confirmOrder(confirm.value.order.id)
    confirm.value.open=false
    await load()
  }finally{confirm.value.loading=false}
}
function pay(order){router.push({path:'/pay',query:{orderNo:order.orderNo}})}
watch(status,()=>{page.value=1;load()})
watch(page,load)
onMounted(load)
</script>

<template>
  <div class="orders-page">
    <header><small>MY ORDERS</small><h1>我的订单</h1></header>
    <OrderStatusTabs v-model="status"/>
    <PixelSkeleton v-if="loading" :rows="6"/>
    <PixelErrorState v-else-if="failed" :image="network" @retry="load"/>
    <PixelEmptyState v-else-if="!orders.length" :image="empty" title="这里还没有订单" desc="去集市逛逛，也许会发现喜欢的好物。" cta="去逛逛" @cta-click="router.push('/products')"/>
    <section v-else class="order-list">
      <OrderCard v-for="order in orders" :key="order.id" :order="order" @pay="pay(order)" @cancel="ask('cancel',order)"
        @confirm="ask('confirm',order)" @detail="router.push(`/user/orders/${order.id}`)"/>
    </section>
    <PixelPagination v-if="total>10" v-model:page="page" :total="total" :page-size="10"/>
    <PixelConfirmDialog v-model="confirm.open" :danger="confirm.type==='cancel'" :loading="confirm.loading"
      :title="confirm.type==='cancel'?'取消订单':'确认收货'" :content="confirm.type==='cancel'?'取消后库存将释放，确定继续吗？':'请确认已经收到商品。'" @confirm="execute" @cancel="confirm.open=false"/>
  </div>
</template>

<style scoped>.orders-page{display:grid;gap:var(--sp-4)}header small{color:var(--c-moss-dark)}header h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.order-list{display:grid;gap:var(--sp-4)}</style>
