<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/engagement'
import { useUserStore } from '@/stores/user'
import PixelToast from '@/components/ui/PixelToast'
import heart from '@/assets/pixel/icons/icon-heart.png'

const props = defineProps({ productId: { type: [Number, String], required: true } })
const user = useUserStore()
const route = useRoute()
const router = useRouter()
const favorite = ref(false)
const loading = ref(false)

async function load() {
  if (!user.token || !props.productId) {
    favorite.value = false
    return
  }
  favorite.value = Boolean(await getFavoriteStatus(props.productId).catch(() => false))
}

async function toggle() {
  if (!user.token) {
    return router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
  loading.value = true
  try {
    if (favorite.value) {
      await removeFavorite(props.productId)
      favorite.value = false
      PixelToast.success('已取消收藏')
    } else {
      await addFavorite(props.productId)
      favorite.value = true
      PixelToast.success('已加入收藏')
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.productId, load, { immediate: true })
</script>

<template>
  <button
    type="button"
    class="favorite-button"
    :class="{ active: favorite }"
    :aria-pressed="favorite"
    :disabled="loading"
    :title="favorite ? '取消收藏' : '收藏商品'"
    @click="toggle"
  >
    <img :src="heart" alt="" data-pixel>
    <span>{{ favorite ? '已收藏' : '收藏' }}</span>
  </button>
</template>

<style scoped>
.favorite-button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:40px;padding:0 12px;border:3px solid var(--c-wood-dark);background:var(--c-surface-2);color:var(--c-ink);box-shadow:var(--shadow-pixel);cursor:pointer}.favorite-button img{width:22px;height:22px;filter:grayscale(1)}.favorite-button.active{border-color:var(--c-brick);color:var(--c-brick);background:var(--c-surface)}.favorite-button.active img{filter:none}.favorite-button:disabled{opacity:.6;cursor:wait}
</style>
