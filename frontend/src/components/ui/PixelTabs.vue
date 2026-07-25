<script setup>
defineProps({ modelValue: { type: [String, Number], default: '' }, items: { type: Array, default: () => [] } })
defineEmits(['update:modelValue', 'change'])
</script>
<template>
  <div class="pixel-tabs" role="tablist">
    <button v-for="item in items" :key="item.value" type="button" role="tab"
      :aria-selected="modelValue === item.value" :class="{ active: modelValue === item.value }"
      @click="$emit('update:modelValue', item.value);$emit('change', item.value)">{{ item.label }}</button>
  </div>
</template>
<style scoped>
.pixel-tabs{display:flex;gap:var(--sp-2);overflow:auto;padding:2px 2px 6px}.pixel-tabs button{min-height:40px;padding:0 14px;border:2px solid var(--c-wood);border-radius:var(--radius);background:var(--c-surface);color:var(--c-ink);white-space:nowrap;cursor:pointer}.pixel-tabs button.active{background:var(--c-moss);border-color:var(--c-wood-dark);color:var(--c-surface);box-shadow:var(--shadow-pixel)}.pixel-tabs button:focus-visible{outline:2px solid var(--c-amber);outline-offset:2px}
</style>
