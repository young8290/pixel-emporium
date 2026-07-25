<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryTree } from '@/api/category'
import { listProducts } from '@/api/product'
import CategorySidebar from '@/components/business/CategorySidebar.vue'
import FilterBar from '@/components/business/FilterBar.vue'
import ProductCard from '@/components/business/ProductCard.vue'
import PixelPagination from '@/components/ui/PixelPagination.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import emptySearch from '@/assets/pixel/empty-states/empty-search.png'
import network from '@/assets/pixel/illustrations/error-network.png'
const route=useRoute(),router=useRouter(),categories=ref([]),products=ref([]),total=ref(0),loading=ref(true),failed=ref(false)
const q=(key,fallback='')=>route.query[key]??fallback
function change(patch){router.push({path:route.path,query:{...route.query,...patch,page:patch.page??1}})}
async function load(){loading.value=true;failed.value=false;try{const page=await listProducts({page:Number(q('page',1)),size:12,keyword:q('keyword')||undefined,categoryId:q('categoryId')||undefined,minPrice:q('minPrice')||undefined,maxPrice:q('maxPrice')||undefined,inStock:q('inStock')==='true'?true:undefined,sort:q('sort','default')});products.value=page?.list||[];total.value=Number(page?.total||0)}catch{failed.value=true}finally{loading.value=false}}
onMounted(async()=>{categories.value=await getCategoryTree().catch(()=>[]);load()})
watch(()=>route.query,load,{deep:true})
</script>
<template><div class="list-page"><header class="page-heading"><small>{{ q('keyword')?'SEARCH RESULTS':'PIXEL EMPORIUM' }}</small><h1>{{ q('keyword')?`“${q('keyword')}” 的搜索结果`:'全部商品' }}</h1><p>共找到 {{ total }} 件精选好物</p></header><div class="catalog"><CategorySidebar :categories="categories" :active="q('categoryId')" @select="change({categoryId:$event||undefined})"/><section><FilterBar :sort="q('sort','default')" :min-price="q('minPrice')" :max-price="q('maxPrice')" :in-stock="q('inStock')==='true'" @update:sort="change({sort:$event})" @apply="change($event)"/><PixelSkeleton v-if="loading" cards :rows="8"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><PixelEmptyState v-else-if="!products.length" :image="emptySearch" title="没有找到合适的商品" desc="试试换个关键词、价格或分类。"/><template v-else><div class="product-grid"><ProductCard v-for="product in products" :key="product.id" :product="product"/></div><PixelPagination :page="Number(q('page',1))" :total="total" :size="12" @change="change({page:$event})"/></template></section></div></div></template>
<style scoped>.list-page{padding-top:var(--sp-3)}.page-heading{margin-bottom:var(--sp-4)}.page-heading small{color:var(--c-moss-dark);letter-spacing:.12em}.page-heading h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.page-heading p{color:var(--c-ink-2)}.catalog{display:grid;grid-template-columns:220px minmax(0,1fr);gap:var(--sp-4)}.catalog section{display:grid;align-content:start;gap:var(--sp-4)}@media(max-width:767px){.catalog{grid-template-columns:1fr}.catalog :deep(.category-side){display:flex;overflow:auto}.catalog :deep(.category-side h2){display:none}.catalog :deep(.category-side button){min-width:max-content;border-left:0;border-bottom:3px solid transparent}.catalog :deep(.category-side button.active){border-bottom-color:var(--c-moss)}}</style>
