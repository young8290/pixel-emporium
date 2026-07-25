<script setup>
import { ref, watch } from 'vue'
import { getProductRecommendations } from '@/api/product'
import ProductCard from '@/components/business/ProductCard.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'

const props = defineProps({ productId: { type: [Number, String], required: true } })
const products = ref([])
const loading = ref(false)
let requestSequence = 0

async function load() {
  if (!props.productId) return
  const sequence = ++requestSequence
  loading.value = true
  try {
    const result = await getProductRecommendations(props.productId, 8)
    if (sequence === requestSequence) products.value = Array.isArray(result) ? result : []
  } catch {
    if (sequence === requestSequence) products.value = []
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

watch(() => props.productId, load, { immediate: true })
</script>

<template>
  <section v-if="loading || products.length" class="recommendations">
    <header><small>YOU MAY ALSO LIKE</small><h2>猜你喜欢</h2></header>
    <PixelSkeleton v-if="loading" cards :rows="4" />
    <div v-else class="product-grid">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
  </section>
</template>

<style scoped>
.recommendations{display:grid;gap:var(--sp-4);margin-top:var(--sp-6);padding-top:var(--sp-5);border-top:3px solid var(--c-line)}header small{color:var(--c-moss-dark)}h2{font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}.product-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--sp-4)}@media(max-width:900px){.product-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:680px){.product-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
