<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAddresses } from '@/api/address'
import { createOrder } from '@/api/order'
import { getProduct } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelTextarea from '@/components/ui/PixelTextarea.vue'
import AddressCard from '@/components/business/AddressCard.vue'
import OrderItemList from '@/components/business/OrderItemList.vue'
import emptyAddress from '@/assets/pixel/empty-states/empty-address.png'
import network from '@/assets/pixel/illustrations/error-network.png'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const source = computed(() => route.query.source === 'buyNow' ? 'buyNow' : 'cart')
const addresses = ref([])
const selectedAddressId = ref(null)
const items = ref([])
const loading = ref(true)
const failed = ref(false)
const submitting = ref(false)
const remark = ref('')
const amount = computed(() => items.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0))

async function load() {
  loading.value = true
  failed.value = false
  try {
    const addressPage = await listAddresses()
    addresses.value = addressPage?.records || addressPage?.list || addressPage || []
    selectedAddressId.value = (addresses.value.find((item) => item.isDefault) || addresses.value[0])?.id || null
    if (source.value === 'cart') {
      await cart.fetch()
      const requested = String(route.query.cartItemIds || '').split(',').filter(Boolean)
      items.value = cart.items.filter((item) => requested.length ? requested.includes(String(item.id)) : item.checked).filter((item) => !item.invalid)
    } else {
      const detail = await getProduct(route.query.productId)
      const sku = (detail?.skus || []).find((item) => String(item.id) === String(route.query.skuId))
      if (sku) items.value = [{
        id: sku.id,
        productSkuId: sku.id,
        productTitle: detail.product?.title,
        skuTitle: sku.title,
        image: sku.image || detail.images?.[0]?.url || detail.product?.image,
        price: sku.price,
        quantity: Math.max(1, Number(route.query.quantity || 1))
      }]
    }
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!selectedAddressId.value || !items.value.length || submitting.value) return
  submitting.value = true
  try {
    const payload = { addressId: selectedAddressId.value, source: source.value, remark: remark.value.trim() }
    if (source.value === 'cart') payload.cartItemIds = items.value.map((item) => item.id)
    else Object.assign(payload, { skuId: route.query.skuId, quantity: Number(route.query.quantity || 1) })
    const orderNo = await createOrder(payload)
    await router.replace({ path: '/pay', query: { orderNo: typeof orderNo === 'string' ? orderNo : orderNo?.orderNo } })
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="checkout-page">
    <header class="page-heading"><small>CHECKOUT</small><h1>确认订单</h1></header>
    <PixelSkeleton v-if="loading" :rows="7" />
    <PixelErrorState v-else-if="failed" :image="network" @retry="load" />
    <template v-else>
      <section class="checkout-section">
        <div class="section-title"><h2>选择收货地址</h2><PixelButton type="ghost" size="small" @click="router.push('/user/address')">管理地址</PixelButton></div>
        <PixelEmptyState v-if="!addresses.length" :image="emptyAddress" title="还没有收货地址" cta="新增地址" @cta-click="router.push('/user/address')" />
        <div v-else class="address-grid">
          <AddressCard v-for="address in addresses" :key="address.id" :address="address" selectable
            :selected="String(selectedAddressId) === String(address.id)" @select="selectedAddressId = address.id" />
        </div>
      </section>
      <section class="checkout-section">
        <h2>商品清单</h2>
        <PixelEmptyState v-if="!items.length" title="没有可结算商品" desc="请返回购物篮重新选择。" cta="返回购物篮" @cta-click="router.replace('/cart')" />
        <OrderItemList v-else :items="items" />
      </section>
      <section class="checkout-section checkout-note">
        <label>给商家留言<PixelTextarea v-model="remark" :maxlength="255" show-word-limit placeholder="选填，最多 255 字" /></label>
        <dl><div><dt>商品合计</dt><dd>¥{{ amount.toFixed(2) }}</dd></div><div><dt>运费</dt><dd>下单时确认</dd></div><div class="total"><dt>应付</dt><dd>¥{{ amount.toFixed(2) }}</dd></div></dl>
      </section>
      <footer class="submit-bar"><span>最终金额以后端订单为准</span><PixelButton :loading="submitting" :disabled="!selectedAddressId || !items.length" @click="submit">提交订单</PixelButton></footer>
    </template>
  </div>
</template>

<style scoped>
.checkout-page{padding-top:var(--sp-4);padding-bottom:90px}.page-heading{margin-bottom:var(--sp-4)}.page-heading small{color:var(--c-moss-dark)}.page-heading h1,.checkout-section h2{font-family:var(--font-pixel);color:var(--c-wood-dark)}.checkout-section{display:grid;gap:var(--sp-3);margin-bottom:var(--sp-4);padding:var(--sp-4);border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.section-title{display:flex;align-items:center;justify-content:space-between}.address-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-3)}.checkout-note{grid-template-columns:minmax(0,1fr) 320px}.checkout-note label{display:grid;gap:8px}.checkout-note dl{display:grid;gap:8px}.checkout-note dl>div{display:flex;justify-content:space-between}.checkout-note .total{padding-top:10px;border-top:2px solid var(--c-line);font-size:20px;color:var(--c-amber)}.submit-bar{position:sticky;z-index:20;bottom:12px;display:flex;align-items:center;justify-content:flex-end;gap:18px;padding:12px 16px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel-lg)}.submit-bar span{color:var(--c-ink-2)}@media(max-width:767px){.address-grid,.checkout-note{grid-template-columns:1fr}.submit-bar{position:fixed;left:0;right:0;bottom:60px;border-width:3px 0}.submit-bar span{font-size:12px}}
</style>
