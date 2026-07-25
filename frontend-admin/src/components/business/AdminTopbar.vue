<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import logoutIcon from '@/assets/pixel/icons/icon-logout.png'
const route=useRoute(),router=useRouter(),user=useUserStore()
const title=computed(()=>route.meta.title||'管理后台')
function logout(){user.clearUserInfo();router.replace('/login')}
</script>
<template><header class="topbar"><div><small>像素百货 /</small><b>{{ title }}</b></div><div class="account"><span>{{ user.email }}</span><button title="退出登录" @click="logout"><img :src="logoutIcon" alt="退出" data-pixel></button></div></header></template>
<style scoped>.topbar{display:flex;align-items:center;justify-content:space-between;height:56px;padding:0 20px;border-bottom:3px solid var(--c-wood);background:var(--c-surface)}.topbar>div{display:flex;align-items:center;gap:8px}.topbar small{color:var(--c-ink-2)}.topbar b{font-family:var(--font-pixel);color:var(--c-wood-dark)}.account span{color:var(--c-ink-2)}.account button{display:grid;place-items:center;width:42px;height:42px;border:2px solid var(--c-wood);background:var(--c-surface-2);cursor:pointer}.account img{width:22px}@media(max-width:520px){.topbar small,.account span{display:none}}</style>
