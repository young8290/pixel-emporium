<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, shipOrder } from '@/api/business/order'
import StatusTag from '@/components/business/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)
const shipping = ref(false)
const shipForm = reactive({ shippingCarrier: '', trackingNumber: '', shippingNote: '' })
const canShip = computed(() => shipForm.shippingCarrier.trim() && shipForm.trackingNumber.trim())

async function load() {
  loading.value = true
  try { order.value = await getOrder(route.params.id) } finally { loading.value = false }
}

async function ship() {
  if (!canShip.value || shipping.value) return
  await ElMessageBox.confirm(`确认通过 ${shipForm.shippingCarrier.trim()} 发出该订单？`, '确认发货', { type: 'warning' })
  shipping.value = true
  try {
    await shipOrder(order.value.id, {
      shippingCarrier: shipForm.shippingCarrier,
      trackingNumber: shipForm.trackingNumber,
      shippingNote: shipForm.shippingNote
    })
    ElMessage.success('订单已发货')
    await load()
  } finally {
    shipping.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <header><div><small>ORDER DETAIL</small><h1>订单详情</h1></div><el-button @click="router.back()">返回</el-button></header>
    <template v-if="order">
      <section class="panel overview">
        <div><span>订单号</span><b>{{ order.orderNo }}</b></div>
        <div><span>用户 ID</span><b>{{ order.userId }}</b></div>
        <div><span>状态</span><StatusTag :status="order.status" /></div>
        <div><span>下单时间</span><b>{{ order.createdAt }}</b></div>
      </section>
      <section class="panel"><h2>收货地址</h2><p>{{ order.receiverName }}　{{ order.receiverPhone }}</p><p>{{ order.receiverProvince }}{{ order.receiverCity }}{{ order.receiverDistrict }}{{ order.receiverDetail }}</p></section>
      <section v-if="order.status === 'PAID'" class="panel shipping-form">
        <h2>填写物流信息</h2>
        <el-input v-model="shipForm.shippingCarrier" maxlength="50" placeholder="物流公司" />
        <el-input v-model="shipForm.trackingNumber" maxlength="100" placeholder="运单号" />
        <el-input v-model="shipForm.shippingNote" maxlength="255" placeholder="发货备注（选填）" />
        <el-button type="success" :loading="shipping" :disabled="!canShip" @click="ship">确认发货</el-button>
      </section>
      <section v-if="order.shippingCarrier || order.trackingNumber" class="panel shipping-detail">
        <h2>物流信息</h2>
        <p><span>物流公司</span><b>{{ order.shippingCarrier }}</b></p>
        <p><span>运单号</span><b>{{ order.trackingNumber }}</b></p>
        <p v-if="order.shippingNote"><span>发货备注</span><b>{{ order.shippingNote }}</b></p>
      </section>
      <section class="panel"><h2>商品明细</h2><el-table :data="order.items || []"><el-table-column prop="productTitle" label="商品" /><el-table-column prop="skuTitle" label="规格" /><el-table-column prop="price" label="单价" /><el-table-column prop="quantity" label="数量" /><el-table-column label="小计"><template #default="{ row }">¥{{ (Number(row.price) * Number(row.quantity)).toFixed(2) }}</template></el-table-column></el-table></section>
      <section class="panel amount"><p>商品：¥{{ Number(order.totalAmount || 0).toFixed(2) }}</p><p>运费：¥{{ Number(order.freightAmount || 0).toFixed(2) }}</p><strong>应付：¥{{ Number(order.payAmount || 0).toFixed(2) }}</strong></section>
    </template>
  </div>
</template>

<style scoped>
.page { display: grid; gap: 18px; min-height: 300px; }
header { display: flex; justify-content: space-between; align-items: center; }
header small { color: var(--c-moss-dark); }
h1, h2 { font-family: var(--font-pixel); color: var(--c-wood-dark); }
.panel { padding: 18px; border: 3px solid var(--c-wood-dark); background: var(--c-surface); box-shadow: var(--shadow-pixel); }
.overview { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.overview > div { display: grid; gap: 5px; }
.overview span, .shipping-detail span { color: var(--c-ink-2); }
.shipping-form { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)) auto; gap: 12px; align-items: end; }
.shipping-form h2 { grid-column: 1 / -1; }
.shipping-detail { display: grid; gap: 10px; }
.shipping-detail p { display: grid; grid-template-columns: 100px minmax(0, 1fr); gap: 12px; }
.amount { display: flex; justify-content: flex-end; align-items: center; gap: 20px; }
.amount strong { font-size: 20px; color: var(--c-amber); }
@media (max-width: 800px) {
  .overview { grid-template-columns: repeat(2, 1fr); }
  .shipping-form { grid-template-columns: 1fr; }
  .shipping-form h2 { grid-column: auto; }
  .amount { flex-wrap: wrap; }
}
</style>
