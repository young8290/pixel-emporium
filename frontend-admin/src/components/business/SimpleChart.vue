<script setup>
import { computed } from 'vue'
const props=defineProps({values:{type:Array,default:()=>[]},labels:{type:Array,default:()=>[]}})
const points=computed(()=>{const max=Math.max(...props.values,1);return props.values.map((v,i)=>`${10+i*(280/Math.max(props.values.length-1,1))},${110-Number(v)/max*90}`).join(' ')})
</script>
<template><section class="chart"><h3>近 7 日订单趋势</h3><svg viewBox="0 0 300 130" role="img" aria-label="近 7 日订单趋势图"><path d="M10 110H290" class="axis"/><polyline :points="points" class="line"/><circle v-for="(v,i) in values" :key="i" :cx="10+i*(280/Math.max(values.length-1,1))" :cy="110-Number(v)/Math.max(...values,1)*90" r="4"/><text v-for="(l,i) in labels" :key="l" :x="10+i*(280/Math.max(labels.length-1,1))" y="127">{{ l }}</text></svg></section></template>
<style scoped>.chart{padding:18px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}h3{font-family:var(--font-pixel);color:var(--c-wood-dark)}svg{width:100%;height:220px}.axis{stroke:var(--c-line);stroke-width:2}.line{fill:none;stroke:var(--c-moss);stroke-width:4;stroke-linejoin:bevel}circle{fill:var(--c-amber);stroke:var(--c-wood-dark);stroke-width:2}text{fill:var(--c-ink-2);font-size:8px;text-anchor:middle}</style>
