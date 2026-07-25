<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import spring from '@/assets/pixel/backgrounds/wallpaper/wall-spring.png'
import summer from '@/assets/pixel/backgrounds/wallpaper/wall-summer.png'
import autumn from '@/assets/pixel/backgrounds/wallpaper/wall-autumn.png'
import winter from '@/assets/pixel/backgrounds/wallpaper/wall-winter.png'

const theme = useThemeStore()
const wallpapers = { spring, summer, autumn, winter }
const current = ref(theme.resolvedSeason)
const previous = ref('')
let releaseTimer

watch(() => theme.resolvedSeason, (next, old) => {
  if (next === old) return
  previous.value = old
  current.value = next
  window.clearTimeout(releaseTimer)
  releaseTimer = window.setTimeout(() => {
    previous.value = ''
  }, 240)
})

onBeforeUnmount(() => window.clearTimeout(releaseTimer))
</script>

<template>
  <div class="wallpaper-layer" aria-hidden="true">
    <i
      v-if="previous"
      class="wallpaper-layer__season wallpaper-layer__season--previous"
      :data-season="previous"
      :style="{ '--season-wallpaper': `url(${wallpapers[previous]})` }"
    />
    <i
      :key="current"
      class="wallpaper-layer__season wallpaper-layer__season--current"
      :data-season="current"
      :style="{ '--season-wallpaper': `url(${wallpapers[current]})` }"
    />
    <i class="wallpaper-layer__scrim" />
  </div>
</template>

<style scoped>
.wallpaper-layer,
.wallpaper-layer__season {
  position: fixed;
  inset: 0;
  pointer-events: none;
}
.wallpaper-layer { z-index: 0; overflow: hidden; background: var(--c-bg); }
.wallpaper-layer__season {
  background: var(--season-wallpaper) 0 0 / 256px 256px repeat;
  image-rendering: pixelated;
}
.wallpaper-layer__season--previous { z-index: 0; }
.wallpaper-layer__scrim {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: color-mix(in srgb, var(--c-bg) 28%, transparent);
  pointer-events: none;
}
:global(:root[data-mode="night"]) .wallpaper-layer__scrim { background: color-mix(in srgb, var(--c-bg) 52%, transparent); }
.wallpaper-layer__season--current {
  z-index: 1;
  animation: wallpaper-dissolve 240ms steps(6, end) both;
  mask: var(--transition-dissolve-mask) 0 0 / 64px 64px repeat;
}
@keyframes wallpaper-dissolve {
  from { opacity: 0; mask-position: 64px 0; }
  to { opacity: 1; mask-position: 0 0; }
}
@media (prefers-reduced-motion: reduce) {
  .wallpaper-layer__season--current { animation: none; mask: none; }
}
</style>
