<script setup>
const props=defineProps({modelValue:{type:Number,default:1},min:{type:Number,default:1},max:{type:Number,default:99},disabled:Boolean})
const emit=defineEmits(['update:modelValue','change'])
function set(v){const next=Math.min(props.max,Math.max(props.min,v));emit('update:modelValue',next);emit('change',next)}
</script>
<template><div class="stepper"><button :disabled="disabled||modelValue<=min" aria-label="减少" @click="set(modelValue-1)">−</button><input :value="modelValue" type="number" :min="min" :max="max" :disabled="disabled" aria-label="数量" @change="set(Number($event.target.value))"><button :disabled="disabled||modelValue>=max" aria-label="增加" @click="set(modelValue+1)">＋</button></div></template>
<style scoped>
.stepper{display:inline-grid;grid-template-columns:40px 54px 40px;height:40px}.stepper button,.stepper input{border:2px solid var(--c-wood);background:var(--c-surface);color:var(--c-ink);font:inherit;text-align:center}.stepper button{font-size:20px;cursor:pointer}.stepper button:disabled{background:var(--c-disabled);cursor:not-allowed}.stepper input{border-left:0;border-right:0;min-width:0;-moz-appearance:textfield}.stepper input::-webkit-inner-spin-button{appearance:none}
</style>
