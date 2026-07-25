<script setup>
defineProps({ title: { type: String, default: '' }, padding: { type: Boolean, default: true }, interactive: Boolean })
</script>
<template>
  <section class="pixel-card pixel-nine-card" :class="{ 'has-padding': padding, interactive }">
    <header v-if="title || $slots.header" class="pixel-card__header"><slot name="header">{{ title }}</slot></header>
    <slot />
  </section>
</template>
<style scoped>
.pixel-card{position:relative;background:var(--c-surface);color:var(--c-ink);box-shadow:var(--shadow-pixel)}.has-padding{padding:var(--sp-4)}.pixel-card__header{margin:calc(-1 * var(--sp-2)) 0 var(--sp-4);font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}.interactive{transition:transform var(--dur-1),box-shadow var(--dur-1)}.interactive:hover{transform:translateY(calc(-1 * var(--rise)));box-shadow:var(--shadow-pixel-lg)}
</style>
