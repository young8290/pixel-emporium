<script setup>
defineProps({ rows: { type: Number, default: 4 }, cards: Boolean })
</script>
<template><div :class="['pixel-skeleton',{cards}]" aria-label="内容加载中"><i v-for="n in rows" :key="n" /></div></template>
<style scoped>
.pixel-skeleton{display:grid;gap:var(--sp-3)}.pixel-skeleton.cards{grid-template-columns:repeat(auto-fit,minmax(180px,1fr))}.pixel-skeleton i{display:block;min-height:48px;border:2px solid var(--c-line);background:linear-gradient(90deg,var(--c-surface-2),var(--c-surface),var(--c-surface-2));background-size:200% 100%;animation:shimmer 1.2s linear infinite}.pixel-skeleton.cards i{min-height:260px}@keyframes shimmer{to{background-position:-200% 0}}@media(prefers-reduced-motion:reduce){.pixel-skeleton i{animation:none}}
</style>
