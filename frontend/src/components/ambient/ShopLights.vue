<script setup>
import { useThemeStore } from '@/stores/theme'
import lantern from '@/assets/pixel/common/dec-lantern.png'

const theme = useThemeStore()
// 顶部均匀垂挂 4 盏常亮灯；每盏在下方投一片暖光池，把内容区照亮
const lamps = [{ x: '12%' }, { x: '38%' }, { x: '62%' }, { x: '88%' }]
</script>

<template>
  <div v-if="theme.resolvedMode === 'night'" class="shop-lights" aria-hidden="true">
    <div
      v-for="(lamp, index) in lamps"
      :key="index"
      class="shop-lights__unit"
      :style="{ '--x': lamp.x }"
    >
      <i class="shop-lights__glow" />
      <img class="shop-lights__lamp" :src="lantern" alt="" data-pixel draggable="false">
    </div>
  </div>
</template>

<style scoped>
.shop-lights { position: absolute; z-index: 2; inset: 0; pointer-events: none; }
.shop-lights__unit { position: absolute; top: 150px; left: var(--x); transform: translateX(-50%); }
.shop-lights__lamp {
  position: relative;
  z-index: 1;
  display: block;
  width: 44px;
  height: auto;
  image-rendering: pixelated;
  transform-origin: top center;
  filter: drop-shadow(0 0 9px color-mix(in srgb, var(--c-glow) 82%, transparent));
  animation: shop-lamp-sway 4.4s ease-in-out infinite;
}
.shop-lights__unit:nth-child(even) .shop-lights__lamp { animation-delay: -2.2s; }
.shop-lights__glow {
  position: absolute;
  left: 50%;
  top: 26px;
  width: min(520px, 58vw);
  aspect-ratio: 1;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--c-glow) 34%, transparent) 0 12%,
    color-mix(in srgb, var(--c-glow) 16%, transparent) 40%,
    transparent 72%
  );
}
@keyframes shop-lamp-sway {
  0%, 100% { transform: rotate(-2deg); }
  50% { transform: rotate(2deg); }
}
@media (prefers-reduced-motion: reduce) {
  .shop-lights__lamp { animation: none; }
}
@media (max-width: 767px) {
  /* 窄屏只留两盏，避免拥挤 */
  .shop-lights__unit:nth-child(even) { display: none; }
  .shop-lights__lamp { width: 32px; }
}
</style>
