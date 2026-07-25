<script setup>
defineProps({ label: { type: String, required: true }, disabled: Boolean, size: { type: Number, default: 44 } })
defineEmits(['click'])
</script>
<template>
  <button class="icon-btn" type="button" :disabled="disabled" :aria-label="label"
    :style="{ width: `${size}px`, height: `${size}px` }" @click="$emit('click', $event)">
    <slot />
  </button>
</template>
<style scoped>
.icon-btn{display:inline-grid;place-items:center;padding:8px;border:2px solid var(--c-wood-dark);border-radius:var(--radius);background:var(--c-surface);box-shadow:var(--shadow-pixel);cursor:pointer;transition:transform var(--dur-1),background var(--dur-1)}
.icon-btn:hover:not(:disabled){transform:translateY(calc(-1 * var(--rise)));background:var(--c-surface-2)}
.icon-btn:active:not(:disabled){transform:translateY(var(--press));box-shadow:none}.icon-btn:focus-visible{outline:2px solid var(--c-amber);outline-offset:2px}.icon-btn:disabled{opacity:.55;cursor:not-allowed}
</style>
