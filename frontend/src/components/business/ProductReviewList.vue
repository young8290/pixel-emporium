<script setup>
import StarRating from './StarRating.vue'

defineProps({
  reviews: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false }
})

defineEmits(['retry'])
</script>

<template>
  <div class="review-list" aria-live="polite">
    <div v-if="loading" class="reviews-loading" role="status">正在读取购买评价...</div>
    <div v-else-if="error" class="reviews-error" role="alert">
      <p>评价暂时加载失败</p>
      <button type="button" @click="$emit('retry')">重新加载</button>
    </div>
    <div v-else-if="!reviews.length" class="reviews-empty" role="status">
      <p>还没有购买评价</p>
    </div>
    <div v-else>
      <article v-for="review in reviews" :key="review.id" class="review-row">
        <header>
          <div>
            <strong>{{ review.reviewerLabel || '已购用户' }}</strong>
            <span v-if="review.verifiedPurchase">已验证购买</span>
          </div>
          <time>{{ review.createdAt }}</time>
        </header>
        <StarRating :model-value="Number(review.rating) || 0" readonly />
        <p class="review-content">{{ review.content }}</p>
        <small v-if="review.skuTitle">规格：{{ review.skuTitle }}</small>
      </article>
    </div>
  </div>
</template>

<style scoped>
.review-list{min-height:120px}.reviews-loading,.reviews-error,.reviews-empty{display:grid;justify-items:center;align-content:center;gap:12px;min-height:160px;color:var(--c-ink-2)}.reviews-error button{height:40px;padding:0 16px;border:2px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-moss);color:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer}.review-row{display:grid;gap:10px;padding:var(--sp-4) 0;border-bottom:1px solid var(--c-line)}.review-row header{display:flex;justify-content:space-between;gap:16px;align-items:start}.review-row header>div{display:flex;align-items:center;gap:10px}.review-row header strong{font-family:var(--font-pixel);font-size:16px;color:var(--c-wood-dark)}.review-row header span{padding:2px 6px;border:1px solid var(--c-moss-dark);color:var(--c-moss-dark);font-size:12px}.review-row time,.review-row small{color:var(--c-ink-2);font-size:12px}.review-content{font-family:var(--font-sans);font-size:14px;line-height:1.75;white-space:pre-wrap;overflow-wrap:anywhere}@media(max-width:560px){.review-row header{display:grid}.review-row header>div{flex-wrap:wrap}}
</style>
