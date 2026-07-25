<script setup>
import { computed, ref, watch } from 'vue'
import placeholder from '@/assets/pixel/products/product-placeholder.png'
const props=defineProps({images:{type:Array,default:()=>[]},title:{type:String,default:'商品图'}})
const index=ref(0), original=ref(false)
watch(()=>props.images,()=>{index.value=0})
const current=computed(()=>props.images[index.value]||{})
const src=computed(()=>original.value?(current.value.originalUrl||current.value.url||placeholder):(current.value.styledUrl||current.value.url||current.value.originalUrl||placeholder))
</script>
<template><section class="gallery"><div class="main-image"><Transition name="fade"><img :key="src" :src="src" :alt="title" data-pixel></Transition><button v-if="current.styledUrl" @click="original=!original">{{ original?'看像素风':'看原图' }}</button></div><div class="thumbs"><button v-for="(image,i) in images" :key="image.id||i" :class="{active:i===index}" @click="index=i;original=false"><img :src="image.styledUrl||image.url||image.originalUrl||placeholder" alt="" data-pixel></button></div></section></template>
<style scoped>
.gallery{display:grid;gap:10px}.main-image{position:relative;aspect-ratio:1;border:4px solid var(--c-wood-dark);background:var(--c-surface);overflow:hidden}.main-image>img{position:absolute;width:100%;height:100%;object-fit:contain}.main-image>button{position:absolute;right:10px;bottom:10px;min-height:38px;padding:0 12px;border:2px solid var(--c-wood-dark);background:var(--c-surface);color:var(--c-ink);cursor:pointer}.thumbs{display:flex;gap:8px;overflow:auto}.thumbs button{flex:0 0 66px;height:66px;padding:2px;border:3px solid var(--c-line);background:var(--c-surface);cursor:pointer}.thumbs button.active{border-color:var(--c-amber)}.thumbs img{width:100%;height:100%;object-fit:cover}.fade-enter-active,.fade-leave-active{transition:opacity var(--dur-2)}.fade-enter-from,.fade-leave-to{opacity:0}
</style>
