<script setup>
defineProps({ image: { type: String, default: '' }, title: { type: String, default: '这里还是空的' }, desc: { type: String, default: '' }, cta: { type: String, default: '' } })
defineEmits(['ctaClick'])
</script>
<template>
  <section class="state" role="status"><img v-if="image" :src="image" :alt="title" loading="lazy" data-pixel><h2>{{ title }}</h2><p v-if="desc">{{ desc }}</p><button v-if="cta" type="button" @click="$emit('ctaClick')">{{ cta }}</button></section>
</template>
<style scoped>
.state{display:grid;justify-items:center;gap:var(--sp-3);padding:var(--sp-6);text-align:center}.state img{width:min(280px,80%);max-height:220px;object-fit:contain}.state h2{font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}.state p{color:var(--c-ink-2)}.state button{min-height:44px;padding:0 18px;border:2px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-moss);color:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer}
</style>
