<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useThemeStore } from '@/stores/theme'
import lanternLit from '@/assets/pixel/interactive/lantern-lit.png'
import lanternOff from '@/assets/pixel/interactive/lantern-off.png'

const theme = useThemeStore()
const position = ref({
  x: theme.lantern.x ?? Math.max(32, window.innerWidth - 96),
  y: theme.lantern.y ?? 144
})
const dragging = ref(false)
let activePointer = null
let moved = false
let pendingPosition = null
let animationFrame = 0

const overlayStyle = computed(() => ({
  '--lantern-x': `${position.value.x}px`,
  '--lantern-y': `${position.value.y}px`
}))

function clampPosition(x, y) {
  return {
    x: Math.min(Math.max(32, x), Math.max(32, window.innerWidth - 32)),
    y: Math.min(Math.max(48, y), Math.max(48, window.innerHeight - 48))
  }
}

function flushPendingPosition() {
  animationFrame = 0
  if (!pendingPosition) return
  position.value = pendingPosition
  pendingPosition = null
}

function onPointerMove(event) {
  if (event.pointerId !== activePointer) return
  moved ||= Math.abs(event.clientX - position.value.x) > 3 || Math.abs(event.clientY - position.value.y) > 3
  pendingPosition = clampPosition(event.clientX, event.clientY)
  if (!animationFrame) animationFrame = window.requestAnimationFrame(flushPendingPosition)
}

function removePointerListeners() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
}

function onPointerUp(event) {
  if (event.pointerId !== activePointer) return
  if (animationFrame) window.cancelAnimationFrame(animationFrame)
  animationFrame = 0
  if (Number.isFinite(event.clientX) && Number.isFinite(event.clientY)) {
    pendingPosition = clampPosition(event.clientX, event.clientY)
  }
  flushPendingPosition()
  theme.setLantern(position.value)
  dragging.value = false
  activePointer = null
  removePointerListeners()
}

function onPointerDown(event) {
  if (event.button !== 0) return
  activePointer = event.pointerId
  moved = false
  dragging.value = true
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerup', onPointerUp, { passive: true })
  window.addEventListener('pointercancel', onPointerUp, { passive: true })
}

function toggleLantern() {
  if (moved) {
    moved = false
    return
  }
  theme.setLantern({ on: !theme.lantern.on })
}

onBeforeUnmount(() => {
  if (animationFrame) window.cancelAnimationFrame(animationFrame)
  removePointerListeners()
})
</script>

<template>
  <!-- 夜幕：留在内容之后，只压暗背景不盖住内容 -->
  <div v-if="theme.resolvedMode === 'night'" class="night-overlay" aria-hidden="true">
    <i class="night-overlay__veil" />
  </div>
  <!-- 可移动的自由灯 + 它的光晕：提到内容之上，随处可拖动并照亮 -->
  <div
    v-if="theme.resolvedMode === 'night'"
    class="night-lantern-layer"
    :style="overlayStyle"
    aria-hidden="true"
  >
    <i v-if="theme.lantern.on" class="night-overlay__glow" />
    <button
      class="night-overlay__lantern is-draggable"
      :class="{ 'is-dragging': dragging }"
      type="button"
      data-testid="lantern"
      :aria-label="theme.lantern.on ? '拖动灯笼或点击熄灯' : '拖动灯笼或点击点灯'"
      @pointerdown="onPointerDown"
      @click="toggleLantern"
    >
      <img :src="theme.lantern.on ? lanternLit : lanternOff" alt="" data-pixel draggable="false">
    </button>
  </div>
</template>

<style scoped>
/* 夜幕层：内容之后 */
.night-overlay { position: fixed; z-index: 2; inset: 0; pointer-events: none; }
.night-overlay__veil {
  position: absolute;
  inset: 0;
  background: var(--night-veil);
  backdrop-filter: saturate(.95);
  pointer-events: none;
}
/* 自由灯层：内容之上，仅灯笼本身可交互 */
.night-lantern-layer { position: fixed; z-index: 50; inset: 0; pointer-events: none; }
.night-overlay__glow {
  position: absolute;
  left: var(--lantern-x);
  top: var(--lantern-y);
  width: min(300px, 38vw);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--c-glow) 34%, transparent) 0 12%, color-mix(in srgb, var(--c-glow) 12%, transparent) 42%, transparent 72%);
  transform: translate(-50%, -45%);
  pointer-events: none;
}
.night-overlay__lantern {
  position: absolute;
  left: var(--lantern-x);
  top: var(--lantern-y);
  width: 64px;
  height: 96px;
  padding: 0;
  border: 0;
  background: transparent;
  transform: translate(-50%, -50%);
  touch-action: none;
  pointer-events: auto;
  filter: drop-shadow(0 0 8px color-mix(in srgb, var(--c-glow) 72%, transparent));
}
.night-overlay__lantern img { display: block; width: 64px; height: 96px; image-rendering: pixelated; pointer-events: none; }
.night-overlay__lantern.is-dragging { filter: drop-shadow(0 0 14px var(--c-glow)); }
@media (prefers-reduced-motion: reduce) { .night-overlay__lantern { transition: none; } }
</style>
