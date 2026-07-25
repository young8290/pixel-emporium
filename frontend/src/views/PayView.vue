<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createAlipayPayment } from '@/api/pay'
import { submitAlipayForm } from '@/utils/alipay'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelLoading from '@/components/ui/PixelLoading.vue'
import wallet from '@/assets/pixel/icons/icon-wallet.png'
import network from '@/assets/pixel/illustrations/error-network.png'

const route = useRoute()
const router = useRouter()
const payment = ref(null)
const loading = ref(true)
const failed = ref(false)
const launching = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    payment.value = await createAlipayPayment(route.query.orderNo)
    if (!payment.value?.formHtml) {
      await router.replace({ path: '/pay/result', query: { orderNo: route.query.orderNo } })
    }
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function launch() {
  if (launching.value || !payment.value?.formHtml) return
  launching.value = true
  try {
    submitAlipayForm(payment.value.formHtml)
  } catch {
    failed.value = true
    launching.value = false
  }
}

onMounted(() => route.query.orderNo ? load() : router.replace('/user/orders'))
</script>

<template>
  <main class="pay-page">
    <section v-if="loading" class="pay-card"><PixelLoading :size="34" /><p>正在准备支付宝沙箱支付……</p></section>
    <PixelErrorState v-else-if="failed" :image="network" title="支付单创建失败" @retry="load" />
    <section v-else class="pay-card">
      <img :src="wallet" alt="" data-pixel><small>ALIPAY SANDBOX</small><h1>支付宝沙箱收银台</h1>
      <p>订单号：{{ payment.orderNo }}</p><strong>¥{{ Number(payment.amount).toFixed(2) }}</strong>
      <p class="hint">仅用于实训演示，请使用支付宝沙箱买家账号，不会产生真实扣款。</p>
      <PixelButton :loading="launching" block @click="launch">前往支付宝支付</PixelButton>
    </section>
  </main>
</template>

<style scoped>.pay-page{display:grid;place-items:center;min-height:62vh;padding:var(--sp-5) 0}.pay-card{display:grid;justify-items:center;gap:14px;width:min(520px,100%);padding:var(--sp-6);border:4px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel-lg);text-align:center}.pay-card>img{width:64px}.pay-card small{color:var(--c-moss-dark)}.pay-card h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.pay-card strong{font-family:var(--font-pixel);font-size:38px;color:var(--c-amber)}.hint{padding:10px;background:var(--c-surface-2);color:var(--c-ink-2)}</style>
