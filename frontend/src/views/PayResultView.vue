<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaymentStatus, queryAlipayPayment } from '@/api/pay'
import PayStatusCard from '@/components/business/PayStatusCard.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelLoading from '@/components/ui/PixelLoading.vue'
import network from '@/assets/pixel/illustrations/error-network.png'

const route = useRoute()
const router = useRouter()
const payment = ref(null)
const waiting = ref(true)
const failed = ref(false)
const querying = ref(false)
let timer
let attempts = 0
const settled = (data) => data?.status === 'SUCCESS' || ['PAID','SHIPPED','COMPLETED','CANCELED'].includes(data?.orderStatus)

async function check() {
  failed.value = false
  try {
    payment.value = await getPaymentStatus(route.query.orderNo)
    if (settled(payment.value) || ++attempts >= 6) {
      waiting.value = false
      clearInterval(timer)
    }
  } catch {
    failed.value = true
    waiting.value = false
    clearInterval(timer)
  }
}
function retryPay(){router.replace({path:'/pay',query:{orderNo:route.query.orderNo}})}
async function queryAlipay(){
  if(querying.value)return
  clearInterval(timer)
  querying.value=true
  failed.value=false
  try{
    payment.value=await queryAlipayPayment(route.query.orderNo)
    waiting.value=false
  }catch{
    failed.value=true
    waiting.value=false
  }finally{
    querying.value=false
  }
}
onMounted(async()=>{if(!route.query.orderNo)return router.replace('/user/orders');await check();if(waiting.value)timer=setInterval(check,1500)})
onBeforeUnmount(()=>clearInterval(timer))
</script>

<template>
  <main class="result-page">
    <section v-if="waiting" class="waiting"><PixelLoading :size="36"/><h1>正在确认订单……</h1><p>请不要关闭页面</p></section>
    <PixelErrorState v-else-if="failed" :image="network" title="结果查询失败" @retry="check"/>
    <PayStatusCard v-else :success="payment?.status==='SUCCESS' || ['PAID','SHIPPED','COMPLETED'].includes(payment?.orderStatus)" :order="payment||{}">
      <PixelButton type="ghost" @click="router.push('/products')">继续购物</PixelButton>
      <PixelButton v-if="payment?.status==='SUCCESS'" @click="router.push('/user/orders')">查看订单</PixelButton>
      <template v-else>
        <PixelButton type="ghost" :loading="querying" @click="queryAlipay">向支付宝查询</PixelButton>
        <PixelButton type="brick" @click="retryPay">重新支付</PixelButton>
      </template>
    </PayStatusCard>
  </main>
</template>

<style scoped>.result-page{min-height:62vh;padding:var(--sp-4) 0}.waiting{display:grid;place-items:center;gap:12px;padding:80px 20px;text-align:center}.waiting h1{font-family:var(--font-pixel);color:var(--c-wood-dark)}</style>
