<script setup>
/**
 * PixelLoading —— 自研像素 spinner（8 个方块围成一圈，整体步进旋转）
 * 仅动 transform（容器 rotate，steps 步进营造像素跳格感）。
 * reduce 下停转，改为静态低对比占位（不闪）。
 */
defineProps({
  /** 直径像素 @type {number} */
  size: { type: Number, default: 24 },
  /** 描述文案（可选，显示在下方） @type {string} */
  label: { type: String, default: '' }
})
</script>

<template>
  <span class="pixel-loading" role="status" :aria-label="label || '加载中'">
    <span
      class="pixel-loading__ring"
      :style="{ width: size + 'px', height: size + 'px' }"
    >
      <i
        v-for="n in 8"
        :key="n"
        class="pixel-loading__dot"
        :style="{
          transform: `rotate(${(n - 1) * 45}deg) translateY(-${size * 0.36}px)`,
          opacity: n / 8
        }"
      />
    </span>
    <span v-if="label" class="pixel-loading__label">{{ label }}</span>
  </span>
</template>

<style scoped>
.pixel-loading {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-2);
  color: var(--c-ink-2);
  font-size: var(--fs-sm);
}
.pixel-loading__ring {
  position: relative;
  display: inline-block;
  animation: pixel-spin 0.8s steps(8) infinite;
}
.pixel-loading__dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 22%;
  height: 22%;
  margin: -11% 0 0 -11%;
  background: var(--c-moss);
  transform-origin: center;
  image-rendering: pixelated;
}
.pixel-loading__label {
  color: var(--c-ink-2);
}

@keyframes pixel-spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .pixel-loading__ring { animation: none; }
  .pixel-loading__dot { opacity: 0.6; }
}
</style>
