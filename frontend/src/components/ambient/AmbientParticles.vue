<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useThemeStore } from '@/stores/theme'
import firefly from '@/assets/pixel/interactive/particle-firefly.png'
import petal from '@/assets/pixel/interactive/particle-petal.png'
import leaf from '@/assets/pixel/interactive/particle-leaf.png'
import snow from '@/assets/pixel/interactive/particle-snow.png'

const theme = useThemeStore()
const particleImages = { spring: petal, summer: firefly, autumn: leaf, winter: snow }
const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
const reduceMotion = ref(motionQuery.matches)
const viewportWidth = ref(window.innerWidth)

function onMotionChange(event) {
  reduceMotion.value = event.matches
}

function onResize() {
  viewportWidth.value = window.innerWidth
}

motionQuery.addEventListener?.('change', onMotionChange)
window.addEventListener('resize', onResize, { passive: true })

const count = computed(() => viewportWidth.value <= 767 ? 12 : 24)
const particles = computed(() => reduceMotion.value ? [] : Array.from({ length: count.value }, (_, index) => ({
  id: index,
  left: `${(index * 37 + 11) % 97}%`,
  delay: `${-((index * 0.73) % 8).toFixed(2)}s`,
  duration: `${6 + (index % 5) * 1.1}s`,
  drift: `${(index % 2 ? 1 : -1) * (12 + (index % 4) * 7)}px`
})))

onBeforeUnmount(() => {
  motionQuery.removeEventListener?.('change', onMotionChange)
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <div v-if="particles.length" class="ambient-particles" aria-hidden="true">
    <i
      v-for="particle in particles"
      :key="particle.id"
      class="ambient-particles__item"
      :style="{
        '--particle-left': particle.left,
        '--particle-delay': particle.delay,
        '--particle-duration': particle.duration,
        '--particle-drift': particle.drift
      }"
    >
      <img :src="particleImages[theme.resolvedSeason]" alt="" data-pixel>
    </i>
  </div>
</template>

<style scoped>
.ambient-particles { position: fixed; z-index: 3; inset: 0; overflow: hidden; pointer-events: none; }
.ambient-particles__item {
  position: absolute;
  top: -24px;
  left: var(--particle-left);
  width: 16px;
  height: 16px;
  animation: particle-drift var(--particle-duration) linear var(--particle-delay) infinite;
}
.ambient-particles__item img { display: block; width: 16px; height: 16px; image-rendering: pixelated; }
@keyframes particle-drift {
  0% { opacity: 0; transform: translate3d(0, -24px, 0) rotate(0); }
  12% { opacity: .7; }
  88% { opacity: .55; }
  100% { opacity: 0; transform: translate3d(var(--particle-drift), calc(100vh + 48px), 0) rotate(180deg); }
}
@media (prefers-reduced-motion: reduce) { .ambient-particles { display: none; } }
</style>
