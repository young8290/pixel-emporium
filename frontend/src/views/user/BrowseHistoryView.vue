<script setup>
import { onMounted, ref } from 'vue'
import { clearBrowseHistory, listBrowseHistory, removeBrowseHistory } from '@/api/engagement'
import ProductCard from '@/components/business/ProductCard.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelConfirmDialog from '@/components/ui/PixelConfirmDialog.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelPagination from '@/components/ui/PixelPagination.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelToast from '@/components/ui/PixelToast'
import empty from '@/assets/pixel/empty-states/empty-shelf.png'
import history from '@/assets/pixel/icons/icon-history.png'
import network from '@/assets/pixel/illustrations/error-network.png'

const page = ref(1)
const items = ref([])
const total = ref(0)
const loading = ref(true)
const failed = ref(false)
const removing = ref(null)
const clearDialog = ref(false)
const clearing = ref(false)

async function load() {
  loading.value = true
  failed.value = false
  try {
    const result = await listBrowseHistory({ page: page.value, size: 12 })
    items.value = result?.list || []
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
    await removeBrowseHistory(productId)
    if (items.value.length === 1 && page.value > 1) page.value -= 1
    await load()
    PixelToast.success('已删除浏览记录')
  } finally {
    removing.value = null
  }
}

async function clear() {
  clearing.value = true
  try {
    await clearBrowseHistory()
    clearDialog.value = false
    page.value = 1
    await load()
    PixelToast.success('浏览记录已清空')
  } finally {
    clearing.value = false
  }
}

function changePage(nextPage) {
  page.value = nextPage
  load()
}

function viewedAt(value) {
  return value || '刚刚浏览'
}

onMounted(load)
</script>

<template>
  <div class="saved-page">
    <header>
      <div><small>RECENTLY VIEWED</small><h1>浏览历史</h1></div>
      <PixelButton data-test="open-clear-history" type="danger" size="small" :disabled="!items.length" @click="clearDialog = true">清空历史</PixelButton>
    </header>
    <PixelSkeleton v-if="loading" cards :rows="8" />
    <PixelErrorState v-else-if="failed" :image="network" @retry="load" />
    <PixelEmptyState v-else-if="!items.length" :image="empty" title="还没有浏览记录" desc="浏览过的商品会出现在这里，方便继续比较。" cta="去逛逛" @cta-click="$router.push('/products')" />
    <template v-else>
      <section class="history-grid">
        <article v-for="item in items" :key="item.historyId || item.id" class="history-item">
          <ProductCard :product="item" />
          <footer><span><img :src="history" alt="" data-pixel> 浏览 {{ item.viewCount }} 次</span><time>{{ viewedAt(item.lastViewedAt) }}</time><button type="button" :disabled="removing === item.id" title="删除浏览记录" @click="remove(item.id)">删除</button></footer>
        </article>
      </section>
      <PixelPagination :page="page" :total="total" :size="12" @change="changePage" />
    </template>
    <PixelConfirmDialog :model-value="clearDialog" title="清空浏览历史" content="清空后无法恢复，确定继续吗？" danger :loading="clearing" @update:model-value="clearDialog = $event" @cancel="clearDialog = false" @confirm="clear" />
  </div>
</template>

<style scoped>
.saved-page{display:grid;gap:var(--sp-4)}header{display:flex;align-items:center;justify-content:space-between;gap:12px}header small{color:var(--c-moss-dark)}h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.history-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:var(--sp-4)}.history-item{display:grid;gap:8px;min-width:0}.history-item footer{display:flex;align-items:center;gap:7px;color:var(--c-ink-2);font-size:12px}.history-item footer span{display:inline-flex;align-items:center;gap:4px}.history-item footer img{width:18px;height:18px}.history-item time{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.history-item button{margin-left:auto;border:0;background:transparent;color:var(--c-brick);cursor:pointer}.history-item button:disabled{opacity:.6;cursor:wait}@media(max-width:960px){.history-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:680px){.history-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.history-item time{display:none}}
</style>
