<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import sceneDay from '@/assets/pixel/auth/auth-scene-day.png'
import sceneNight from '@/assets/pixel/auth/auth-scene-night.png'
import signOpen from '@/assets/pixel/auth/auth-sign-open.png'
import cat from '@/assets/pixel/auth/auth-cat.png'
import lamp from '@/assets/pixel/auth/auth-lamp.png'

const theme = useThemeStore()
const sceneSource = computed(() => theme.resolvedMode === 'night' ? sceneNight : sceneDay)
</script>

<template>
  <section class="auth-scene" aria-hidden="true">
    <img
      class="auth-scene__backdrop auth-scene__backdrop--complete"
      :src="sceneSource"
      alt=""
      data-pixel
    >
    <img class="auth-scene__sign" :src="signOpen" alt="" data-pixel>
    <img class="auth-scene__cat" :src="cat" alt="" data-pixel>
    <img v-if="theme.resolvedMode === 'night'" class="auth-scene__lamp" :src="lamp" alt="" data-pixel>
  </section>
</template>

<style scoped>
.auth-scene {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 0;
  overflow: hidden;
  background: var(--c-surface-2);
  isolation: isolate;
}
.auth-scene__backdrop {
  display: block;
  width: 100%;
  height: 100%;
  object-view-box: inset(30% 12% 0% 12%);
  object-fit: cover;
  object-position: center bottom;
  image-rendering: pixelated;
}
.auth-scene__sign,
.auth-scene__cat,
.auth-scene__lamp {
  position: absolute;
  z-index: 1;
  pointer-events: none;
  image-rendering: pixelated;
}
.auth-scene__sign {
  top: 12%;
  left: 23%;
  width: 64px;
  height: 48px;
  transform-origin: center top;
  animation: auth-sign-sway 4s var(--ease-std) infinite alternate;
}
.auth-scene__cat {
  bottom: 11%;
  left: 31%;
  width: 48px;
  height: 48px;
  animation: auth-cat-nap 4s steps(2, end) infinite;
}
.auth-scene__lamp {
  right: 22%;
  bottom: 19%;
  width: 48px;
  height: 80px;
  animation: auth-lamp-glow 2s var(--ease-std) infinite alternate;
}
@keyframes auth-sign-sway {
  from { transform: rotate(-1.5deg); }
  to { transform: rotate(1.5deg); }
}
@keyframes auth-cat-nap {
  from { transform: translateY(0); }
  to { transform: translateY(-1px); }
}
@keyframes auth-lamp-glow {
  from { filter: drop-shadow(0 0 3px var(--c-glow)); opacity: .82; }
  to { filter: drop-shadow(0 0 8px var(--c-glow)); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-scene__sign,
  .auth-scene__cat,
  .auth-scene__lamp { animation: none; }
}
</style>
