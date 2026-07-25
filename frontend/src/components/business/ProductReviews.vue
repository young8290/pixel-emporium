<script setup>
import { ref, watch } from 'vue'
import { getReviewSummary, listProductReviews } from '@/api/review'
import PixelPagination from '@/components/ui/PixelPagination.vue'
import ProductReviewList from './ProductReviewList.vue'
import ProductReviewSummary from './ProductReviewSummary.vue'

const props = defineProps({
  productId: { type: [String, Number], required: true }
})

const pageSize = 10
const summary = ref({ averageRating: 0, reviewCount: 0, distribution: {} })
const reviews = ref([])
const total = ref(0)
const page = ref(1)
const sort = ref('newest')
const rating = ref('')
const loading = ref(false)
const error = ref(false)

async function loadPage() {
  if (!props.productId) return
  loading.value = true
  error.value = false
  try {
    const result = await listProductReviews(props.productId, {
      page: page.value,
      size: pageSize,
      sort: sort.value,
      rating: rating.value ? Number(rating.value) : undefined
    })
    reviews.value = result?.list || []
    total.value = Number(result?.total) || 0
  } catch {
    reviews.value = []
    total.value = 0
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  if (!props.productId) return
  const summaryRequest = getReviewSummary(props.productId)
    .then((result) => { summary.value = result || summary.value })
    .catch(() => {})
  await Promise.all([summaryRequest, loadPage()])
}

function changeFilters() {
  page.value = 1
  loadPage()
}

function changePage(next) {
  page.value = next
  loadPage()
}

watch(() => props.productId, () => {
  page.value = 1
  loadAll()
}, { immediate: true })
</script>

<template>
  <section class="product-reviews" aria-labelledby="product-reviews-title">
    <header class="reviews-heading">
      <div>
        <small>VERIFIED PURCHASES</small>
        <h2 id="product-reviews-title">商品评价</h2>
      </div>
      <div class="review-controls">
        <label>
          <span>星级</span>
          <select v-model="rating" data-test="review-rating" @change="changeFilters">
            <option value="">全部</option>
            <option v-for="star in [5,4,3,2,1]" :key="star" :value="String(star)">{{ star }} 星</option>
          </select>
        </label>
        <label>
          <span>排序</span>
          <select v-model="sort" data-test="review-sort" @change="changeFilters">
            <option value="newest">最新</option>
            <option value="highest">评分最高</option>
            <option value="lowest">评分最低</option>
          </select>
        </label>
      </div>
    </header>
    <ProductReviewSummary :summary="summary" />
    <ProductReviewList :reviews="reviews" :loading="loading" :error="error" @retry="loadPage" />
    <PixelPagination
      v-if="total > pageSize"
      :page="page"
      :size="pageSize"
      :total="total"
      @change="changePage"
    />
  </section>
</template>

<style scoped>
.product-reviews{width:100%;margin-top:var(--sp-6);padding:var(--sp-5) 0;border-top:3px solid var(--c-wood-dark);border-bottom:3px solid var(--c-wood-dark)}.reviews-heading{display:flex;justify-content:space-between;align-items:end;gap:var(--sp-4)}.reviews-heading small{color:var(--c-moss-dark);font-size:12px}.reviews-heading h2{margin-top:4px;font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark);letter-spacing:0}.review-controls{display:flex;gap:12px}.review-controls label{display:grid;gap:4px;color:var(--c-ink-2);font-size:12px}.review-controls select{width:132px;height:40px;padding:0 28px 0 10px;border:2px solid var(--c-wood);border-radius:var(--radius);background:var(--c-surface);color:var(--c-ink);cursor:pointer}@media(max-width:640px){.product-reviews{padding:var(--sp-4) 0}.reviews-heading{display:grid}.review-controls{display:grid;grid-template-columns:1fr 1fr;width:100%}.review-controls select{width:100%}}
</style>
