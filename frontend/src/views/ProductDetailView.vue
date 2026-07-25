<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProduct } from '@/api/product'
import { recordBrowseHistory } from '@/api/engagement'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import PixelToast from '@/components/ui/PixelToast'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import Gallery from '@/components/business/Gallery.vue'
import PriceBlock from '@/components/business/PriceBlock.vue'
import ProductFavoriteButton from '@/components/business/ProductFavoriteButton.vue'
import SkuSelector from '@/components/business/SkuSelector.vue'
import QtyStepper from '@/components/business/QtyStepper.vue'
import StockHint from '@/components/business/StockHint.vue'
import AddCartBar from '@/components/business/AddCartBar.vue'
import ProductReviews from '@/components/business/ProductReviews.vue'
import ProductRecommendations from '@/components/business/ProductRecommendations.vue'
import network from '@/assets/pixel/illustrations/error-network.png'
const route=useRoute(),router=useRouter(),cart=useCartStore(),user=useUserStore(),detail=ref(null),selected=ref(null),qty=ref(1),loading=ref(true),failed=ref(false),adding=ref(false)
const product=computed(()=>detail.value?.product||{}),price=computed(()=>selected.value?.price??product.value.price??0),originalPrice=computed(()=>selected.value?.originalPrice??null),stock=computed(()=>Number(selected.value?.stock??0)),disabled=computed(()=>!selected.value||stock.value<1)
const ratingText=computed(()=>Number(product.value.reviewCount)>0?`★ ${Number(product.value.rating||0).toFixed(2)} · ${Number(product.value.reviewCount)} 条评价 · 已售 ${Number(product.value.soldCount||0)}`:`暂无评分 · 已售 ${Number(product.value.soldCount||0)}`)
async function load(){loading.value=true;failed.value=false;try{detail.value=await getProduct(route.params.id);const skus=detail.value?.skus||[];if(skus.length===1||!(detail.value?.attributes||[]).length)selected.value=skus.find(s=>s.stock>0)||skus[0]||null;if(user.token)await recordBrowseHistory(product.value.id).catch(()=>{})}catch(e){failed.value=true;if(e.response?.status===404)router.replace('/404')}finally{loading.value=false}}
async function add(){if(!user.token)return router.push({path:'/login',query:{redirect:route.fullPath}});adding.value=true;try{await cart.add(selected.value.id,qty.value);PixelToast.success('已放进购物篮')}finally{adding.value=false}}
function buy(){if(!user.token)return router.push({path:'/login',query:{redirect:route.fullPath}});router.push({path:'/checkout',query:{source:'buyNow',skuId:selected.value.id,quantity:qty.value,productId:product.value.id}})}
onMounted(load)
</script>
<template><div class="detail-page"><PixelSkeleton v-if="loading" :rows="7"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><template v-else><div class="detail-main"><Gallery :images="detail.images" :title="product.title"/><section class="detail-info"><small>{{ product.categoryName||'精选好物' }}</small><h1>{{ product.title }}</h1><p class="rating">{{ ratingText }}</p><PriceBlock :price="price" :original="originalPrice"/><ProductFavoriteButton :product-id="product.id"/><SkuSelector v-model="selected" :attributes="detail.attributes" :skus="detail.skus" @change="qty=1"/><div class="qty-row"><span>数量</span><QtyStepper v-model="qty" :max="Math.max(1,stock)" :disabled="!selected"/><StockHint :stock="stock"/></div><AddCartBar :disabled="disabled" :loading="adding" @add-cart="add" @buy-now="buy"/></section></div><section class="description pixel-nine-card"><h2>商品详情</h2><div>{{ product.description||'这份好物还没有更多介绍。' }}</div></section><ProductReviews :product-id="product.id"/><ProductRecommendations :product-id="product.id"/></template></div></template>
<style scoped>.detail-page{padding-top:var(--sp-4)}.detail-main{display:grid;grid-template-columns:minmax(0,1fr) minmax(360px,.9fr);gap:var(--sp-6)}.detail-main>*{min-width:0}.detail-info{display:grid;align-content:start;gap:var(--sp-4);padding:var(--sp-5);background:var(--c-surface);border:3px solid var(--c-wood-dark);box-shadow:var(--shadow-pixel)}.detail-info>*{min-width:0}.detail-info>small{color:var(--c-moss-dark)}.detail-info h1{font-family:var(--font-pixel);font-size:30px;line-height:1.35;color:var(--c-wood-dark);overflow-wrap:anywhere}.rating{color:var(--c-amber)}.qty-row{display:flex;align-items:center;gap:16px;padding:14px 0;border-top:2px solid var(--c-line);border-bottom:2px solid var(--c-line)}.description{margin-top:var(--sp-6);padding:var(--sp-5);background:var(--c-surface);white-space:pre-wrap;line-height:1.8}.description h2{margin-bottom:var(--sp-3);font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}@media(max-width:800px){.detail-main{grid-template-columns:minmax(0,1fr)}.detail-info h1{font-size:26px}.description{margin-bottom:76px}}</style>
