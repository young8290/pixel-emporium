<script setup>
import { onMounted, ref } from 'vue'
import { listFavorites, removeFavorite } from '@/api/engagement'
import ProductCard from '@/components/business/ProductCard.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelPagination from '@/components/ui/PixelPagination.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelToast from '@/components/ui/PixelToast'
import empty from '@/assets/pixel/empty-states/empty-favorite.png'
import heart from '@/assets/pixel/icons/icon-heart.png'
import network from '@/assets/pixel/illustrations/error-network.png'

const page = ref(1)
const products = ref([])
const total = ref(0)
const loading = ref(true)
const failed = ref(false)
const removing = ref(null)

async function load() {
  loading.value = true
  failed.value = false
  try {
    const result = await listFavorites({ page: page.value, size: 12 })
    products.value = result?.list || []
    total.value = Number(result?.total || 0)
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function remove(productId) {
  removing.value = productId
  try {
    await removeFavorite(productId)
    if (products.value.length === 1 && page.value > 1) page.value -= 1
    await load()
    PixelToast.success('已取消收藏')
  } finally {
    removing.value = null
  }
}

function changePage(nextPage) {
  page.value = nextPage
  load()
}

onMounted(load)
</script>

<template>
  <div class="saved-page">
    <header>
      <div><small>MY FAVORITES</small><h1>我的收藏</h1></div>
      <PixelButton type="ghost" size="small" @click="load">刷新</PixelButton>
    </header>
    <PixelSkeleton v-if="loading" cards :rows="8" />
    <PixelErrorState v-else-if="failed" :image="network" @retry="load" />
    <PixelEmptyState v-else-if="!products.length" :image="empty" title="收藏夹还是空的" desc="把喜欢的商品收藏起来，方便下次继续挑选。" cta="去逛逛" @cta-click="$router.push('/products')" />
    <template v-else>
      <section class="product-grid">
        <div v-for="product in products" :key="product.id" class="saved-item">
          <ProductCard :product="product" />
          <button type="button" class="remove" data-test="remove-favorite" :disabled="removing === product.id" title="取消收藏" @click.stop="remove(product.id)">
            <img :src="heart" alt="" data-pixel><span>取消收藏</span>
          </button>
        </div>
      </section>
      <PixelPagination :page="page" :total="total" :size="12" @change="changePage" />
    </template>
  </div>
</template>

<style scoped>
.saved-page{display:grid;gap:var(--sp-4)}header{display:flex;align-items:center;justify-content:space-between}header small{color:var(--c-moss-dark)}h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.product-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--sp-4)}.saved-item{position:relative;min-width:0}.remove{position:absolute;z-index:2;top:10px;left:10px;display:inline-flex;align-items:center;gap:4px;min-height:32px;padding:0 7px;border:2px solid var(--c-brick);background:var(--c-surface);color:var(--c-brick);box-shadow:var(--shadow-pixel);cursor:pointer}.remove img{width:18px;height:18px}.remove:disabled{opacity:.6;cursor:wait}@media(max-width:960px){.product-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:680px){.product-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.remove span{display:none}}
</style>
