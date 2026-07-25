<script setup>
import home from '@/assets/pixel/icons/icon-home.png'
import order from '@/assets/pixel/icons/icon-order.png'
import address from '@/assets/pixel/icons/icon-address.png'
import heart from '@/assets/pixel/icons/icon-heart.png'
import history from '@/assets/pixel/icons/icon-history.png'
import user from '@/assets/pixel/icons/icon-user.png'
const items = [
  { to: '/user', label: '概览', icon: home, exact: true },
  { to: '/user/orders', label: '我的订单', icon: order },
  { to: '/user/address', label: '收货地址', icon: address },
  { to: '/user/favorites', label: '我的收藏', icon: heart },
  { to: '/user/history', label: '浏览历史', icon: history },
  { to: '/user/profile', label: '个人资料', icon: user }
]
</script>
<template>
  <nav class="user-menu" aria-label="个人中心菜单">
    <RouterLink v-for="item in items" :key="item.to" :to="item.to" :exact-active-class="item.exact ? 'router-link-active' : ''">
      <img :src="item.icon" alt="" data-pixel><span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>
<style scoped>
.user-menu{display:grid;gap:4px;padding:var(--sp-3);border:4px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.user-menu a{display:flex;align-items:center;gap:10px;min-height:44px;padding:0 12px;border:2px solid transparent;color:var(--c-ink);text-decoration:none}.user-menu a:hover,.user-menu a.router-link-active{border-color:var(--c-wood);background:var(--c-surface-2)}.user-menu img{width:24px;height:24px}
</style>
