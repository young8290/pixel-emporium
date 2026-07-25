<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import SearchBar from './SearchBar.vue'
import ThemeSwitcher from './ThemeSwitcher.vue'
import cartIcon from '@/assets/pixel/icons/icon-cart.png'
import userIcon from '@/assets/pixel/icons/icon-user.png'
import menuIcon from '@/assets/pixel/icons/icon-menu.png'
import basket from '@/assets/pixel/common/dec-basket.png'

defineProps({ mobileMenuOpen: Boolean })
const emit = defineEmits(['toggleMenu'])
const router = useRouter()
const user = useUserStore()
const cart = useCartStore()
const firstName = computed(() => user.email ? user.email.split('@')[0] : '登录')

onMounted(() => {
  if (user.token) cart.fetch().catch(() => {})
})

function search(keyword) {
  router.push({ path: '/search', query: { keyword } })
}

async function signOut() {
  user.clearUserInfo()
  cart.reset()
  await router.push('/')
}
</script>
<template>
  <header class="top-nav">
    <div class="top-nav__inner">
      <button class="mobile-menu" type="button" aria-label="打开菜单" @click="emit('toggleMenu')">
        <img :src="menuIcon" alt="" data-pixel>
      </button>
      <RouterLink class="brand" to="/">
        <img :src="basket" alt="" data-pixel><span>像素百货</span><small>PIXEL EMPORIUM</small>
      </RouterLink>
      <SearchBar class="desktop-search" @search="search" />
      <ThemeSwitcher class="desktop-theme" compact />
      <nav class="top-actions" aria-label="账户与购物车">
        <RouterLink class="action" to="/cart" aria-label="购物车">
          <span class="action__icon"><img :src="cartIcon" alt="" data-pixel><b v-if="cart.count">{{ cart.count > 99 ? '99+' : cart.count }}</b></span>
          <span class="action__label">购物篮</span>
        </RouterLink>
        <el-dropdown v-if="user.token" trigger="click">
          <button class="action user-action" type="button">
            <img :src="userIcon" alt="" data-pixel><span>{{ firstName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/user')">个人中心</el-dropdown-item>
              <el-dropdown-item @click="router.push('/user/orders')">我的订单</el-dropdown-item>
              <el-dropdown-item divided @click="signOut">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <RouterLink v-else class="action" :to="{ path: '/login', query: { redirect: $route.fullPath } }">
          <img :src="userIcon" alt="" data-pixel><span>{{ firstName }}</span>
        </RouterLink>
      </nav>
    </div>
    <div class="mobile-search">
      <SearchBar @search="search" />
      <ThemeSwitcher class="mobile-theme" compact />
    </div>
  </header>
</template>
<style scoped>
.top-nav{position:relative;z-index:30;border-bottom:4px solid var(--c-wood-dark);background:var(--c-wood) url('@/assets/pixel/backgrounds/tex-wood.png');box-shadow:0 3px 0 color-mix(in srgb,var(--c-wood-dark) 35%,transparent)}.top-nav__inner{display:flex;align-items:center;gap:var(--sp-5);width:min(1200px,calc(100% - 48px));height:72px;margin:auto}.brand{display:grid;grid-template-columns:48px auto;grid-template-rows:1fr auto;align-items:center;color:var(--c-surface);text-decoration:none;text-shadow:2px 2px 0 var(--c-wood-dark);white-space:nowrap}.brand img{grid-row:1/3;width:44px;height:44px;margin-right:8px}.brand span{font-family:var(--font-pixel);font-size:24px;line-height:1}.brand small{font-size:9px;letter-spacing:.12em}.desktop-search{margin:auto}.top-actions{display:flex;align-items:center;gap:var(--sp-3)}.action{display:flex;align-items:center;gap:6px;min-height:44px;border:0;background:transparent;color:var(--c-surface);font:inherit;text-decoration:none;cursor:pointer}.action img{width:26px;height:26px}.action__icon{position:relative;display:inline-flex}.action__icon b{position:absolute;right:-10px;top:-9px;min-width:18px;height:18px;padding:0 3px;border:2px solid var(--c-surface);border-radius:10px;background:var(--c-brick);font-size:10px;line-height:14px;text-align:center}.mobile-menu,.mobile-search,.mobile-theme{display:none}.user-action{max-width:120px}.user-action span{overflow:hidden;text-overflow:ellipsis}
@media(max-width:767px){.top-nav__inner{width:calc(100% - 24px);height:60px;gap:10px}.mobile-menu{display:grid;place-items:center;width:44px;height:44px;border:0;background:transparent}.mobile-menu img{width:26px}.brand{grid-template-columns:36px auto}.brand img{width:34px;height:34px}.brand span{font-size:20px}.brand small,.desktop-search,.desktop-theme{display:none}.top-actions{margin-left:auto}.top-actions .action{flex-direction:column;gap:2px}.action__label,.user-action span{display:block;max-width:66px;overflow:hidden;font-size:10px;line-height:1.1;white-space:nowrap;text-overflow:ellipsis}.mobile-search{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:center;gap:8px;padding:8px 12px 12px;background:var(--c-wood)}.mobile-theme{display:inline-flex}}
@media(max-width:520px){.mobile-search{grid-template-columns:1fr}.mobile-theme{justify-self:center}}
</style>
