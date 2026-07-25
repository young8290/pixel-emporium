<script setup>
import { computed, ref, onBeforeUnmount, onMounted } from 'vue'
import defaultBanner from '@/assets/pixel/backgrounds/banner-home.png'
import spring from '@/assets/pixel/backgrounds/banner-home-spring.png'
import summer from '@/assets/pixel/backgrounds/banner-home-summer.png'
import autumn from '@/assets/pixel/backgrounds/banner-home-autumn.png'
import winter from '@/assets/pixel/backgrounds/banner-home-winter.png'
import bunting from '@/assets/pixel/common/dec-bunting.png'
const slides = [defaultBanner, spring, summer, autumn, winter]
const index = ref(0)
let timer
const image = computed(() => slides[index.value])
onMounted(() => {
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    timer = window.setInterval(() => { index.value = (index.value + 1) % slides.length }, 6000)
  }
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>
<template>
  <section class="hero" @mouseenter="clearInterval(timer)" @mouseleave="undefined">
    <Transition name="banner"><img :key="image" class="hero__bg" :src="image" alt="四季像素百货街" data-pixel></Transition>
    <img class="hero__bunting" :src="bunting" alt="" data-pixel>
    <div class="hero__copy"><p>NEW ARRIVALS</p><h1>把精选好物<br>装进你的篮子</h1><span>当季好物 · 品质精选 · 安心送达</span><RouterLink to="/products">去逛商城</RouterLink></div>
    <div class="hero__dots"><button v-for="(_,i) in slides" :key="i" :class="{active:i===index}" :aria-label="`第 ${i+1} 张`" @click="index=i" /></div>
  </section>
</template>
<style scoped>
.hero{position:relative;width:100%;aspect-ratio:1983/793;height:auto;overflow:hidden;border:4px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.hero__bg{position:absolute;inset:0;width:100%;height:100%;object-fit:contain;object-position:center}.hero::after{content:"";position:absolute;inset:0;background:linear-gradient(90deg,color-mix(in srgb,var(--c-ink) 68%,transparent),transparent 62%)}.hero__copy{position:relative;z-index:2;display:grid;align-content:center;gap:10px;width:52%;height:100%;padding:var(--sp-6);color:var(--c-surface);text-shadow:2px 2px 0 var(--c-wood-dark)}.hero__copy p{color:var(--c-amber);font-weight:800}.hero__copy h1{font-family:var(--font-pixel);font-size:36px;line-height:1.35}.hero__copy a{width:max-content;margin-top:8px;padding:11px 20px;border:3px solid var(--c-wood-dark);background:var(--c-moss);color:var(--c-surface);box-shadow:var(--shadow-pixel);text-decoration:none}.hero__bunting{position:absolute;z-index:3;right:20px;top:10px;width:150px}.hero__dots{position:absolute;z-index:4;right:16px;bottom:12px;display:flex;gap:8px}.hero__dots button{width:12px;height:12px;border:2px solid var(--c-surface);background:var(--c-wood);cursor:pointer}.hero__dots button.active{background:var(--c-amber)}.banner-enter-active,.banner-leave-active{transition:opacity var(--dur-3)}.banner-enter-from,.banner-leave-to{opacity:0}@media(max-width:700px){.hero__copy{width:88%;padding:24px}.hero__copy h1{font-size:28px}.hero::after{background:color-mix(in srgb,var(--c-ink) 52%,transparent)}}
</style>
