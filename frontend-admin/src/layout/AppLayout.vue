<script setup>
import { ref } from 'vue'
import AdminSidebar from '@/components/business/AdminSidebar.vue'
import AdminTopbar from '@/components/business/AdminTopbar.vue'
const collapsed=ref(localStorage.getItem('adminSidebar')==='collapsed')
function toggle(){collapsed.value=!collapsed.value;localStorage.setItem('adminSidebar',collapsed.value?'collapsed':'expanded')}
</script>
<template><div class="admin-shell"><AdminSidebar :collapsed="collapsed" @toggle="toggle"/><div class="admin-work"><AdminTopbar/><main><RouterView v-slot="{Component}"><Transition name="page" mode="out-in"><component :is="Component"/></Transition></RouterView></main></div></div></template>
<style scoped>.admin-shell{display:flex;min-height:100vh}.admin-work{min-width:0;flex:1}.admin-work main{padding:24px}.page-enter-active,.page-leave-active{transition:opacity var(--dur-2)}.page-enter-from,.page-leave-to{opacity:0}@media(max-width:767px){.admin-work main{padding:14px}}</style>
