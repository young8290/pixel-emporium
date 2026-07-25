<script setup>
import menuConfig from '@/layout/menuConfig'
import menuIcon from '@/assets/pixel/icons/icon-menu.png'
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
defineProps({collapsed:Boolean})
defineEmits(['toggle'])
const user=useUserStore()
const visibleMenu=computed(()=>menuConfig.filter(item=>!item.roles||item.roles.some(role=>user.roles.includes(role))))
</script>
<template><aside class="sidebar" :class="{collapsed}"><div class="brand"><b>像素百货</b><span>管理后台</span></div><nav><RouterLink v-for="item in visibleMenu" :key="item.path" :to="item.path"><img :src="item.icon" alt="" data-pixel><span>{{ item.title }}</span></RouterLink></nav><button class="toggle" aria-label="折叠菜单" @click="$emit('toggle')"><img :src="menuIcon" alt="" data-pixel><span>{{ collapsed?'展开':'收起' }}</span></button></aside></template>
<style scoped>.sidebar{position:sticky;top:0;display:flex;flex-direction:column;width:224px;height:100vh;padding:14px 10px;border-right:4px solid var(--c-wood-dark);background:var(--c-wood);transition:width var(--dur-2) var(--ease-std)}.sidebar.collapsed{width:64px}.brand{display:grid;place-items:center;min-height:72px;margin-bottom:14px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel);text-align:center;overflow:hidden}.brand b{font-family:var(--font-pixel);font-size:21px;color:var(--c-moss-dark);white-space:nowrap}.brand span{font-size:11px;color:var(--c-ink-2)}nav{display:grid;gap:5px}nav a,.toggle{position:relative;display:flex;align-items:center;gap:12px;min-height:46px;padding:0 12px;border:0;background:transparent;color:var(--c-surface);text-decoration:none;white-space:nowrap;overflow:hidden}nav a::before{content:"";position:absolute;left:0;width:4px;height:0;background:var(--c-amber);transition:height var(--dur-1)}nav a:hover::before,nav a.router-link-active::before{height:70%}nav a:hover,nav a.router-link-active{background:var(--c-moss-dark)}nav img,.toggle img{flex:0 0 auto;width:24px;height:24px}.toggle{margin-top:auto;width:100%;cursor:pointer}.collapsed .brand b,.collapsed .brand span,.collapsed nav span,.collapsed .toggle span{display:none}@media(max-width:767px){.sidebar{width:64px}.brand b,.brand span,nav span,.toggle span{display:none}}</style>
