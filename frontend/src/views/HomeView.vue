<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '@/api/category'
import { listProducts } from '@/api/product'
import Banner from '@/components/business/Banner.vue'
import CategoryNav from '@/components/business/CategoryNav.vue'
import ProductCard from '@/components/business/ProductCard.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import empty from '@/assets/pixel/empty-states/empty-shelf.png'
import network from '@/assets/pixel/illustrations/error-network.png'
const router=useRouter(),categories=ref([]),products=ref([]),loading=ref(true),failed=ref(false)
async function load(){loading.value=true;failed.value=false;try{const [tree,page]=await Promise.all([getCategoryTree(),listProducts({sort:'newest',page:1,size:8})]);categories.value=tree||[];products.value=page?.list||[]}catch{failed.value=true}finally{loading.value=false}}
onMounted(load)
</script>
<template><div class="home"><CategoryNav :categories="categories" active="" @select="router.push({path:'/products',query:$event?{categoryId:$event}:{}})" /><Banner/><section class="home__section"><div class="section-heading"><div><small>NEW ARRIVALS</small><h2>新品上架</h2></div><RouterLink to="/products">查看全部 →</RouterLink></div><PixelSkeleton v-if="loading" cards :rows="4"/><PixelErrorState v-else-if="failed" :image="network" title="商城还没开门" desc="没有取到商品，请稍后再来。" @retry="load"/><PixelEmptyState v-else-if="!products.length" :image="empty" title="正在上新" desc="很快会有新货。" cta="去看看分类" @cta-click="router.push('/products')"/><div v-else class="product-grid"><ProductCard v-for="product in products" :key="product.id" :product="product"/></div></section></div></template>
<style scoped>.home{display:grid;gap:var(--sp-4)}.home__section{padding-top:var(--sp-5)}.section-heading{display:flex;align-items:end;justify-content:space-between;margin-bottom:var(--sp-4);border-bottom:3px solid var(--c-wood);padding-bottom:10px}.section-heading small{color:var(--c-moss-dark);letter-spacing:.12em}.section-heading h2{font-family:var(--font-pixel);font-size:28px;color:var(--c-wood-dark)}.section-heading a{color:var(--c-moss-dark);font-weight:700}</style>
