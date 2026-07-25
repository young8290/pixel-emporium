<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import rope from '@/assets/pixel/common/dec-sign-rope.png'
const route = useRoute()
const router = useRouter()
const dropping = ref('')
const items = [
  { label: '首页', route: '/' },
  { label: '全部商品', route: '/products' },
  { label: '我的订单', route: '/user/orders' },
  { label: '个人中心', route: '/user' }
]
const active = computed(() => {
  const matches = items.filter((item) => item.route === '/' ? route.path === '/' : route.path.startsWith(item.route))
  return matches.sort((a, b) => b.route.length - a.route.length)[0]?.route || ''
})
function navigate(item) {
  if (dropping.value) return
  dropping.value = item.route
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  window.setTimeout(() => {
    dropping.value = ''
    router.push(item.route)
  }, reduced ? 0 : 220)
}
</script>
<template>
  <nav class="sign-nav" aria-label="主导航">
    <div class="sign-rail">
      <button v-for="item in items" :key="item.route" type="button" class="sign"
        :class="{ active: active === item.route, dropping: dropping === item.route }"
        :aria-current="active === item.route ? 'page' : undefined" @click="navigate(item)">
        <img class="rope" :src="rope" alt="" data-pixel><span>{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
<style scoped>
.sign-nav{position:relative;z-index:20;height:82px;background:linear-gradient(var(--c-bg),transparent)}.sign-rail{display:flex;justify-content:center;gap:var(--sp-5);width:min(760px,100%);height:22px;margin:auto;border:4px solid var(--c-wood-dark);border-top:0;background:var(--c-wood)}.sign{position:relative;width:132px;height:72px;border:0;background:transparent;transform-origin:top center;cursor:pointer;transition:transform var(--dur-sign) var(--ease-sign)}.rope{position:absolute;left:50%;top:-8px;width:40px;height:40px;transform:translateX(-50%)}.sign span{position:absolute;left:4px;right:4px;top:23px;display:grid;place-items:center;height:46px;border:3px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-wood) url('@/assets/pixel/borders/sign-hanging-9slice.png');background-size:cover;color:var(--c-surface);font-family:var(--font-pixel);font-size:18px;text-shadow:2px 2px 0 var(--c-wood-dark);box-shadow:var(--shadow-pixel)}.sign:hover:not(.active){transform:translateY(var(--sign-sway)) rotate(var(--sign-tilt))}.sign.active{transform:translateY(calc(var(--sign-sway) / 2))}.sign.active span{box-shadow:0 0 0 3px var(--c-amber),var(--shadow-pixel)}.sign.dropping{animation:sign-drop var(--dur-drop) var(--ease-sign)}.sign:focus-visible{outline:2px solid var(--c-amber);outline-offset:2px}@keyframes sign-drop{50%{transform:translateY(calc(var(--sign-sway) * 2)) rotate(calc(-1 * var(--sign-tilt)))}}@media(max-width:767px){.sign-nav{display:none}}@media(prefers-reduced-motion:reduce){.sign{transition:none}.sign:hover:not(.active),.sign.active{transform:none}.sign.dropping{animation:none}}
</style>
