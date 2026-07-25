<script setup>
import { ref, watch } from 'vue'
import filterIcon from '@/assets/pixel/icons/icon-filter.png'

const props = defineProps({
  sort: { type: String, default: 'default' },
  minPrice: { type: [String, Number], default: '' },
  maxPrice: { type: [String, Number], default: '' },
  inStock: Boolean,
})
const emit = defineEmits(['update:sort', 'apply'])
const minimum = ref(props.minPrice)
const maximum = ref(props.maxPrice)
const stockOnly = ref(props.inStock)
const options=[['default','综合'],['priceAsc','价格 ↑'],['priceDesc','价格 ↓'],['sales','热销'],['rating','评分'],['newest','新品']]

watch(() => [props.minPrice, props.maxPrice, props.inStock], ([minPrice, maxPrice, inStock]) => {
  minimum.value = minPrice
  maximum.value = maxPrice
  stockOnly.value = inStock
})

function apply() {
  emit('apply', {
    minPrice: minimum.value === '' ? undefined : String(minimum.value),
    maxPrice: maximum.value === '' ? undefined : String(maximum.value),
    inStock: stockOnly.value ? true : undefined,
  })
}
</script>
<template><div class="filter-bar"><div class="sorts"><span>排序</span><button v-for="[value,label] in options" :key="value" type="button" :data-sort="value" :class="{active:sort===value}" @click="$emit('update:sort',value)">{{ label }}</button></div><form class="filters" @submit.prevent="apply"><label><span>价格</span><input v-model="minimum" data-test="min-price" type="number" min="0" step="0.01" placeholder="最低"></label><i>–</i><input v-model="maximum" data-test="max-price" type="number" min="0" step="0.01" aria-label="最高价" placeholder="最高"><label class="stock"><input v-model="stockOnly" data-test="in-stock" type="checkbox"><span>仅看有货</span></label><button data-test="apply-filters" class="apply" type="button" @click="apply"><img :src="filterIcon" alt="" data-pixel><span>筛选</span></button></form></div></template>
<style scoped>
.filter-bar{display:grid;gap:8px;padding:10px;border:2px solid var(--c-line);background:var(--c-surface)}.sorts,.filters{display:flex;align-items:center;gap:6px;overflow:auto}.sorts>span,.filters label>span{margin-right:6px;color:var(--c-ink-2);white-space:nowrap}.sorts button{min-height:36px;padding:0 12px;border:2px solid transparent;background:transparent;color:var(--c-ink);cursor:pointer;white-space:nowrap}.sorts button.active{border-color:var(--c-wood);background:var(--c-moss);color:var(--c-surface)}.filters input[type=number]{width:82px;height:34px;padding:0 7px;border:2px solid var(--c-line);background:var(--c-surface-2);color:var(--c-ink)}.filters label{display:flex;align-items:center}.stock{gap:5px;min-height:34px;white-space:nowrap}.stock input{width:18px;height:18px;accent-color:var(--c-moss)}.stock span{margin:0!important;color:var(--c-ink)!important}.apply{display:inline-flex;align-items:center;gap:4px;min-height:34px;padding:0 10px;border:2px solid var(--c-wood-dark);background:var(--c-amber);color:var(--c-wood-dark);cursor:pointer;white-space:nowrap}.apply img{width:20px;height:20px}@media(max-width:680px){.filter-bar{margin-inline:-2px}.sorts,.filters{padding-bottom:2px}}
</style>
