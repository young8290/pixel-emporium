<script setup>
defineProps({ image: { type: String, default: '' }, title: { type: String, default: '路上出了点状况' }, desc: { type: String, default: '请稍后重试' }, retry: { type: String, default: '重试' } })
defineEmits(['retry'])
</script>
<template>
  <section class="error-state" role="alert"><img v-if="image" :src="image" :alt="title" data-pixel><h1>{{ title }}</h1><p>{{ desc }}</p><button v-if="retry" type="button" @click="$emit('retry')">{{ retry }}</button></section>
</template>
<style scoped>
.error-state{display:grid;justify-items:center;gap:var(--sp-3);padding:var(--sp-6);text-align:center}.error-state img{width:min(340px,86%);max-height:260px;object-fit:contain}.error-state h1{font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}.error-state p{color:var(--c-ink-2)}.error-state button{min-height:44px;padding:0 18px;border:2px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-moss);color:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer}
</style>
