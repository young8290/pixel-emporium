<script setup>
import { computed } from 'vue'
import StarRating from './StarRating.vue'

const props = defineProps({
  summary: {
    type: Object,
    default: () => ({ averageRating: 0, reviewCount: 0, distribution: {} })
  }
})

const total = computed(() => Math.max(0, Number(props.summary?.reviewCount) || 0))
const average = computed(() => Math.max(0, Number(props.summary?.averageRating) || 0))
const rows = computed(() => [5, 4, 3, 2, 1].map((star) => {
  const count = Math.max(0, Number(props.summary?.distribution?.[star]) || 0)
  const percent = total.value > 0 ? Math.min(100, (count / total.value) * 100) : 0
  return { star, count, percent }
}))
</script>

<template>
  <div class="review-summary">
    <div class="average-block">
      <strong>{{ total ? average.toFixed(2) : '暂无评分' }}</strong>
      <StarRating :model-value="average" readonly />
      <span>{{ total }} 条真实购买评价</span>
    </div>
    <div class="distribution" aria-label="评分分布">
      <div v-for="row in rows" :key="row.star" class="distribution-row">
        <span>{{ row.star }} 星</span>
        <div class="distribution-track" aria-hidden="true">
          <i class="distribution-fill" :style="{ width: `${row.percent}%` }" />
        </div>
        <b>{{ row.count }}</b>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-summary{display:grid;grid-template-columns:minmax(220px,.75fr) minmax(280px,1.25fr);gap:var(--sp-6);align-items:center;padding:var(--sp-4) 0;border-bottom:2px solid var(--c-line)}.average-block{display:grid;justify-items:start;gap:8px}.average-block strong{font-family:var(--font-pixel);font-size:36px;line-height:1.2;color:var(--c-wood-dark);letter-spacing:0}.average-block span{font-size:14px;color:var(--c-ink-2)}.distribution{display:grid;gap:8px}.distribution-row{display:grid;grid-template-columns:44px minmax(120px,1fr) 32px;align-items:center;gap:10px;font-size:14px;color:var(--c-ink-2)}.distribution-row b{text-align:right;color:var(--c-ink)}.distribution-track{height:10px;border:1px solid var(--c-wood);background:var(--c-surface-2)}.distribution-fill{display:block;height:100%;background:var(--c-amber)}@media(max-width:640px){.review-summary{grid-template-columns:1fr;gap:var(--sp-4)}}
</style>
