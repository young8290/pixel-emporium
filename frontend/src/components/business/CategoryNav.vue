<script setup>
defineProps({ categories: { type: Array, default: () => [] }, active: { type: [String, Number], default: '' } })
defineEmits(['select'])
const iconUrl = (name) => name ? new URL(`../../assets/pixel/categories/${name}`, import.meta.url).href : ''
</script>
<template>
  <nav class="category-nav" aria-label="商品分类">
    <button type="button" :class="{active: active === ''}" @click="$emit('select', '')">全部</button>
    <button v-for="item in categories" :key="item.id" type="button" :class="{active: String(active) === String(item.id)}" @click="$emit('select', item.id)">
      <img v-if="item.icon" :src="iconUrl(item.icon)" alt="" data-pixel @error="$event.target.style.display='none'">{{ item.name }}
    </button>
  </nav>
</template>
<style scoped>
.category-nav{display:flex;gap:var(--sp-2);padding:4px 2px 8px;overflow:auto}.category-nav button{display:flex;align-items:center;gap:6px;min-width:max-content;min-height:42px;padding:0 14px;border:2px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-wood);color:var(--c-surface);box-shadow:var(--shadow-pixel);font:inherit;cursor:pointer}.category-nav button.active,.category-nav button:hover{background:var(--c-moss)}.category-nav img{width:28px;height:28px}
</style>
