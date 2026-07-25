<script setup>
import { computed, reactive, watch } from 'vue'
const props=defineProps({attributes:{type:Array,default:()=>[]},skus:{type:Array,default:()=>[]},modelValue:{type:Object,default:null}})
const emit=defineEmits(['update:modelValue','change'])
const selected=reactive({})
const selectedIds=computed(()=>Object.values(selected).filter(Boolean).map(Number).sort((a,b)=>a-b))
function skuFor(ids){const key=[...ids].sort((a,b)=>a-b).join(',');return props.skus.find(s=>[...(s.attributeValueIds||[])].sort((a,b)=>a-b).join(',')===key)}
function possible(attrId,valueId){const trial=Object.entries(selected).filter(([id])=>Number(id)!==Number(attrId)).map(([,v])=>Number(v)).filter(Boolean).concat(Number(valueId));return props.skus.some(s=>Number(s.stock)>0&&trial.every(id=>(s.attributeValueIds||[]).map(Number).includes(id)))}
function choose(attr,value){if(!possible(attr.id,value.id))return;selected[attr.id]=value.id;if(selectedIds.value.length===props.attributes.length){const sku=skuFor(selectedIds.value);emit('update:modelValue',sku||null);emit('change',sku||null)}else{emit('update:modelValue',null)}}
watch(()=>props.modelValue,(sku)=>{if(!sku)return;for(const attr of props.attributes){const hit=attr.values?.find(v=>(sku.attributeValueIds||[]).map(Number).includes(Number(v.id)));if(hit)selected[attr.id]=hit.id}},{immediate:true})
</script>
<template><div class="sku-selector"><fieldset v-for="attr in attributes" :key="attr.id"><legend>{{ attr.name }}</legend><button v-for="value in attr.values" :key="value.id" type="button" :disabled="!possible(attr.id,value.id)" :class="{active:Number(selected[attr.id])===Number(value.id)}" :aria-pressed="Number(selected[attr.id])===Number(value.id)" @click="choose(attr,value)">{{ value.value }}</button></fieldset><p v-if="attributes.length&&selectedIds.length<attributes.length">请选择每一项规格</p></div></template>
<style scoped>
.sku-selector{display:grid;gap:14px}.sku-selector fieldset{display:flex;align-items:center;gap:8px;flex-wrap:wrap;border:0}.sku-selector legend{float:left;min-width:64px;margin-right:8px;color:var(--c-ink-2)}.sku-selector button{min-height:38px;padding:0 12px;border:2px solid var(--c-wood);background:var(--c-surface);color:var(--c-ink);cursor:pointer;transition:transform var(--dur-1),background var(--dur-1)}.sku-selector button.active{background:var(--c-moss);color:var(--c-surface);transform:translateY(calc(-1 * var(--rise)));box-shadow:var(--shadow-pixel)}.sku-selector button:disabled{border-color:var(--c-line);background:var(--c-disabled);color:var(--c-ink-2);cursor:not-allowed}.sku-selector p{color:var(--c-pumpkin);font-size:12px}
</style>
