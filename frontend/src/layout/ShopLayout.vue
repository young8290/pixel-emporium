<script setup>
import { ref } from 'vue'
import TopNav from '@/components/business/TopNav.vue'
import HangingSignNav from '@/components/business/HangingSignNav.vue'
import Footer from '@/components/business/Footer.vue'
import home from '@/assets/pixel/icons/icon-home.png'
import category from '@/assets/pixel/icons/icon-category.png'
import cart from '@/assets/pixel/icons/icon-cart.png'
import user from '@/assets/pixel/icons/icon-user.png'
const menuOpen = ref(false)
const mobileItems = [
  { to: '/', label: '首页', icon: home },
  { to: '/products', label: '商品', icon: category },
  { to: '/cart', label: '购物车', icon: cart },
  { to: '/user', label: '我的', icon: user }
]
</script>
<template>
  <div class="shop-shell">
    <TopNav :mobile-menu-open="menuOpen" @toggle-menu="menuOpen = !menuOpen" />
    <HangingSignNav />
    <main class="shop-main"><RouterView /></main>
    <Footer />
    <nav class="mobile-tabs" aria-label="移动端导航">
      <RouterLink v-for="item in mobileItems" :key="item.to" :to="item.to">
        <img :src="item.icon" alt="" data-pixel><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>
<style scoped>
.shop-shell{min-height:100vh}.shop-main{width:min(1200px,calc(100% - 48px));min-height:55vh;margin:0 auto}@media(max-width:767px){.shop-main{width:min(100% - 24px,1200px);padding-bottom:64px}.mobile-tabs{position:fixed;z-index:60;left:0;right:0;bottom:0;display:grid;grid-template-columns:repeat(4,1fr);height:60px;border-top:3px solid var(--c-wood-dark);background:var(--c-surface)}.mobile-tabs a{display:grid;place-items:center;align-content:center;gap:2px;color:var(--c-ink-2);font-size:11px;text-decoration:none}.mobile-tabs a.router-link-active{background:var(--c-surface-2);color:var(--c-moss-dark)}.mobile-tabs img{width:24px;height:24px}}@media(min-width:768px){.mobile-tabs{display:none}}
</style>
