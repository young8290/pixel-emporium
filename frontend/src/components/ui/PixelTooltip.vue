<script setup>
defineProps({ content: { type: String, required: true }, placement: { type: String, default: 'top' } })
</script>
<template><el-tooltip :content="content" :placement="placement" popper-class="pixel-tooltip"><slot /></el-tooltip></template>
<style>.pixel-tooltip{background:var(--c-wood-dark)!important;color:var(--c-surface)!important;border:2px solid var(--c-amber)!important}</style>
