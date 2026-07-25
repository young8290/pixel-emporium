<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const phase = ref('idle')
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const timers = new Set()
let lastZone = route.meta.zone || 'shop'

function schedule(callback, delay) {
  const timer = window.setTimeout(() => {
    timers.delete(timer)
    callback()
  }, delay)
  timers.add(timer)
}

function clearTimers() {
  for (const timer of timers) window.clearTimeout(timer)
  timers.clear()
}

watch(() => route.fullPath, () => {
  const zone = route.meta.zone || 'shop'
  const major = zone !== lastZone
  lastZone = zone
  clearTimers()

  // 同区互跳（逛店内部、用户中心换 tab 等）或降级：只做轻微淡入，不放幕布
  if (reducedMotion || !major) {
    phase.value = 'fade'
    schedule(() => { phase.value = 'idle' }, reducedMotion ? 160 : 220)
    return
  }

  // 跨区大跳（进购物流程 / 登录注册 / 用户中心等）：放完整幕布，且比原来更慢更柔
  phase.value = 'cover'
  schedule(() => { phase.value = 'crumble' }, 240)
  schedule(() => { phase.value = 'idle' }, 620)
})

onBeforeUnmount(clearTimers)
</script>

<template>
  <div
    class="page-transition__content"
    :class="{
      'is-changing': phase === 'cover' || phase === 'crumble',
      'is-fading': phase === 'fade'
    }"
  >
    <slot />
  </div>
  <i
    v-if="phase === 'cover' || phase === 'crumble'"
    class="page-transition__curtain"
    :class="`page-transition__curtain--${phase}`"
    aria-hidden="true"
  />
</template>

<style scoped>
.page-transition__content { position: relative; z-index: 10; min-height: 100vh; }
.page-transition__content.is-fading { animation: content-fade var(--dur-page-fade) var(--ease-out) both; }
.page-transition__curtain {
  position: fixed;
  z-index: 70;
  inset: 0;
  background: var(--transition-curtain) 0 0 / 128px 128px repeat;
  image-rendering: pixelated;
  pointer-events: none;
}
.page-transition__curtain--cover { animation: curtain-cover var(--dur-page-cover) steps(6, end) both; }
.page-transition__curtain--crumble {
  animation: curtain-crumble var(--dur-page-crumble) steps(9, end) both;
  mask: var(--transition-dissolve-mask) 0 0 / 64px 64px repeat;
}
@keyframes curtain-cover { from { transform: translateY(-100%); } to { transform: translateY(0); } }
@keyframes curtain-crumble { from { opacity: 1; mask-position: 0 0; } to { opacity: 0; mask-position: 128px 64px; } }
@keyframes content-fade { from { opacity: .6; } to { opacity: 1; } }
</style>
