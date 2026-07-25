<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
import blossom from '@/assets/pixel/common/seasonal/dec-blossom.png'
import pine from '@/assets/pixel/common/seasonal/dec-pine.png'
import pumpkin from '@/assets/pixel/common/seasonal/dec-pumpkin.png'
import flowerpot from '@/assets/pixel/common/dec-flowerpot.png'
import vine from '@/assets/pixel/common/dec-vine.png'
import wheat from '@/assets/pixel/common/dec-wheat.png'

const theme = useThemeStore()

// 四角贴图随季节更换（顺序：左上、右上、左下、右下）
const sets = {
  spring: [blossom, blossom, flowerpot, flowerpot],
  summer: [vine, vine, flowerpot, flowerpot],
  autumn: [wheat, wheat, pumpkin, pumpkin],
  winter: [pine, pine, pine, pine]
}
const slots = ['s-tl', 's-tr', 's-bl', 's-br']
const sprites = computed(() => sets[theme.resolvedSeason] || sets.spring)
</script>

<template>
  <div class="seasonal-decor" aria-hidden="true">
    <img
      v-for="(cls, i) in slots"
      :key="cls"
      class="seasonal-decor__item"
      :class="cls"
      :src="sprites[i]"
      alt=""
      data-pixel
    >
  </div>
</template>

<style scoped>
/* 固定在网页（文档）上，随页面一起滚动；静止不飘 */
.seasonal-decor { position: absolute; z-index: 12; inset: 0; pointer-events: none; }
.seasonal-decor__item {
  position: absolute;
  width: 64px;
  height: 64px;
  image-rendering: pixelated;
  opacity: .96;
  filter: drop-shadow(2px 2px 0 color-mix(in srgb, var(--c-wood-dark) 35%, transparent));
}
.s-tl { top: 100px; left: 6px; }
.s-tr { top: 100px; right: 6px; transform: scaleX(-1); }
.s-bl { bottom: 100px; left: 6px; }
.s-br { bottom: 100px; right: 6px; transform: scaleX(-1); }
@media (max-width: 1200px) {
  .s-tl, .s-tr, .s-bl, .s-br { width: 52px; height: 52px; }
  .s-bl { bottom: 84px; }
  .s-br { bottom: 84px; }
}
@media (max-width: 767px) {
  /* 窄屏空间有限，隐藏以免遮挡内容 */
  .seasonal-decor { display: none; }
}
</style>
