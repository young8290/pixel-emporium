<script setup>
import UserMenu from '@/components/business/UserMenu.vue'
</script>
<template>
  <div class="user-center">
    <aside><UserMenu /></aside>
    <section class="user-center__content"><RouterView /></section>
  </div>
</template>
<style scoped>
.user-center{display:grid;grid-template-columns:220px minmax(0,1fr);gap:var(--sp-5);padding-top:var(--sp-4)}.user-center aside{position:sticky;top:16px;align-self:start}.user-center__content{min-width:0}@media(max-width:767px){.user-center{grid-template-columns:1fr}.user-center aside{position:static}.user-center :deep(.user-menu){grid-template-columns:repeat(4,1fr);overflow:auto}.user-center :deep(.user-menu a){justify-content:center;padding:0 6px}.user-center :deep(.user-menu a span){display:none}}
</style>
