<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import PixelToast from '@/components/ui/PixelToast'
import placeholder from '@/assets/pixel/products/product-placeholder.png'
import cartIcon from '@/assets/pixel/icons/icon-cart.png'
const props = defineProps({ product: { type: Object, required: true } })
const emit = defineEmits(['added'])
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const image = computed(() => props.product.styledUrl || props.product.image || placeholder)
const soldOut = computed(() => props.product.onSale === false || props.product.stock === 0)
async function add(event) {
  event.stopPropagation()
  if (!user.token) return router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  const skuId = props.product.productSkuId || props.product.skuId || props.product.defaultSkuId
  if (!skuId) return router.push(`/product/${props.product.id}`)
  await cart.add(skuId, 1)
  PixelToast.success('已放进购物篮')
  emit('added', event.currentTarget)
}
</script>
<template>
  <article class="product-card" :class="{sold:soldOut}" tabindex="0" @click="router.push(`/product/${product.id}`)" @keydown.enter="router.push(`/product/${product.id}`)">
    <div class="product-card__image"><img :src="image" :alt="product.title" loading="lazy" data-pixel><span v-if="soldOut">暂时售罄</span></div>
    <div class="product-card__body"><small>{{ product.categoryName || '精选好物' }}</small><h3>{{ product.title }}</h3><div class="meta"><strong>¥{{ Number(product.price || 0).toFixed(2) }}</strong><span>已售 {{ product.soldCount || 0 }}</span></div></div>
    <button type="button" aria-label="加入购物车" :disabled="soldOut" @click="add"><img :src="cartIcon" alt="" data-pixel></button>
  </article>
</template>
<style scoped>
.product-card{position:relative;overflow:hidden;border:3px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer;transition:transform var(--dur-1),box-shadow var(--dur-1)}.product-card:hover:not(.sold){transform:translateY(calc(-1 * var(--rise)));box-shadow:var(--shadow-pixel-lg)}.product-card:focus-visible{outline:2px solid var(--c-amber);outline-offset:3px}.product-card__image{position:relative;aspect-ratio:1;background:var(--c-surface-2);overflow:hidden}.product-card__image>img{width:100%;height:100%;object-fit:cover}.product-card__image>span{position:absolute;inset:auto 0 0;padding:8px;background:color-mix(in srgb,var(--c-ink) 78%,transparent);color:var(--c-surface);text-align:center}.product-card__body{display:grid;gap:6px;padding:14px}.product-card small{color:var(--c-moss-dark)}.product-card h3{min-height:44px;overflow:hidden;font-size:16px;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}.meta{display:flex;align-items:end;justify-content:space-between}.meta strong{color:var(--c-amber);font-family:var(--font-pixel);font-size:20px;text-shadow:1px 1px 0 var(--c-wood-dark)}.meta span{font-size:12px;color:var(--c-ink-2)}.product-card>button{position:absolute;right:10px;top:10px;display:grid;place-items:center;width:42px;height:42px;border:2px solid var(--c-wood-dark);background:var(--c-moss);box-shadow:var(--shadow-pixel);cursor:pointer}.product-card>button img{width:24px}.product-card>button:hover:not(:disabled){transform:scale(1.08)}.product-card.sold{filter:grayscale(.65);cursor:not-allowed}.product-card>button:disabled{background:var(--c-disabled);cursor:not-allowed}
</style>
