<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const theme = useThemeStore()
const hasVines = computed(() => ['spring', 'summer'].includes(theme.resolvedSeason))
const corners = ['top-left', 'top-right', 'bottom-left', 'bottom-right']
</script>

<template>
  <div class="vframe" :class="{ 'vframe--vine': hasVines }" aria-hidden="true">
    <i
      v-for="corner in corners"
      :key="corner"
      class="vframe__corner"
      :class="`vframe__corner--${corner}`"
    />
    <i class="vframe__garland" />
    <i class="vframe__baseboard" />
    <i class="vframe__lights vframe__lights--top" />
    <i class="vframe__lights vframe__lights--right" />
    <i class="vframe__lights vframe__lights--bottom" />
    <i class="vframe__lights vframe__lights--left" />
  </div>
</template>

<style scoped>
.vframe { position: fixed; z-index: 45; inset: 0; pointer-events: none; }
.vframe__corner {
  position: absolute;
  width: 96px;
  height: 96px;
  background: var(--frame-corner) center / contain no-repeat;
  image-rendering: pixelated;
}
.vframe--vine .vframe__corner { background-image: var(--frame-corner-vine); }
.vframe__corner--top-left { top: 0; left: 0; }
.vframe__corner--top-right { top: 0; right: 0; transform: scaleX(-1); }
.vframe__corner--bottom-left { bottom: 0; left: 0; transform: scaleY(-1); }
.vframe__corner--bottom-right { right: 0; bottom: 0; transform: scale(-1); }
.vframe__garland {
  position: absolute;
  top: 0;
  left: 50%;
  width: min(512px, 72vw);
  height: 48px;
  background: var(--frame-garland) center top / auto 48px repeat-x;
  transform: translateX(-50%);
  image-rendering: pixelated;
}
.vframe__baseboard {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 32px;
  background: var(--frame-baseboard) center bottom / auto 32px repeat-x;
  image-rendering: pixelated;
}
.vframe__lights {
  position: absolute;
  display: none;
  height: 12px;
  background-image: radial-gradient(circle at 8px 6px, var(--c-glow) 0 4px, var(--c-wood-dark) 4px 5px, transparent 5px);
  background-size: 24px 12px;
  filter: drop-shadow(0 0 6px color-mix(in srgb, var(--c-glow) 85%, transparent));
}
.vframe__lights::before {
  position: absolute;
  top: 1px;
  right: 0;
  left: 0;
  height: 1px;
  content: '';
  background: color-mix(in srgb, var(--c-wood-dark) 78%, transparent);
}
.vframe__lights--top { top: 7px; right: 68px; left: 68px; }
.vframe__lights--bottom { right: 68px; bottom: 37px; left: 68px; }
.vframe__lights--left,
.vframe__lights--right {
  top: 70px;
  bottom: 70px;
  width: 12px;
  height: auto;
  background-size: 12px 32px;
}
.vframe__lights--left { left: 7px; }
.vframe__lights--right { right: 7px; }
:global(:root[data-mode="night"]) .vframe__garland {
  filter: drop-shadow(0 0 4px var(--c-glow));
}
:global(:root[data-mode="night"]) .vframe__lights {
  display: block;
  animation: vframe-lights-pulse 3.2s ease-in-out infinite;
}
@keyframes vframe-lights-pulse {
  0%, 100% { filter: drop-shadow(0 0 5px color-mix(in srgb, var(--c-glow) 70%, transparent)); }
  50% { filter: drop-shadow(0 0 10px color-mix(in srgb, var(--c-glow) 96%, transparent)); }
}
@media (max-width: 767px) {
  .vframe__corner,
  .vframe__baseboard,
  .vframe__lights--bottom,
  .vframe__lights--left,
  .vframe__lights--right { display: none; }
  .vframe__lights--top { right: 16px; left: 16px; }
  .vframe__garland { width: 100%; height: 32px; background-size: auto 32px; }
}
</style>
