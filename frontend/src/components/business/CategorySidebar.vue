<script setup>
defineProps({ categories: { type: Array, default: () => [] }, active: { type: [String, Number], default: '' } })
defineEmits(['select'])
</script>
<template>
  <aside class="category-side"><h2>集市分区</h2><button :class="{active:active===''}" @click="$emit('select','')">全部好物</button>
    <template v-for="item in categories" :key="item.id">
      <button :class="{active:String(active)===String(item.id)}" @click="$emit('select',item.id)">{{ item.name }}</button>
      <button v-for="child in item.children" :key="child.id" class="child" :class="{active:String(active)===String(child.id)}" @click="$emit('select',child.id)">↳ {{ child.name }}</button>
    </template>
  </aside>
</template>
<style scoped>
.category-side{display:grid;align-content:start;padding:var(--sp-3);border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.category-side h2{margin-bottom:8px;padding:8px;border-bottom:2px solid var(--c-line);font-family:var(--font-pixel);font-size:24px;color:var(--c-wood-dark)}.category-side button{min-height:40px;padding:0 10px;border:0;border-left:4px solid transparent;background:transparent;color:var(--c-ink);text-align:left;cursor:pointer}.category-side button:hover,.category-side button.active{border-left-color:var(--c-moss);background:var(--c-surface-2);color:var(--c-moss-dark)}.category-side .child{padding-left:20px;font-size:13px}
</style>
