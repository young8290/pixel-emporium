<script setup>
import PixelTag from '@/components/ui/PixelTag.vue'
defineProps({address:{type:Object,required:true},selectable:Boolean,selected:Boolean})
defineEmits(['select','edit','delete','setDefault'])
const full=(a)=>[a.province,a.city,a.district,a.detail||a.address].filter(Boolean).join(' ')
</script>
<template>
  <article class="address-card" :class="{selected,selectable}" tabindex="0" @click="selectable&&$emit('select')" @keydown.enter="selectable&&$emit('select')">
    <header><strong>{{ address.recipient }}</strong><span>{{ address.phone }}</span><PixelTag v-if="address.isDefault" text="默认" color="moss" /></header>
    <p>{{ full(address) }}</p>
    <footer><button @click.stop="$emit('edit')">编辑</button><button v-if="!address.isDefault" @click.stop="$emit('setDefault')">设为默认</button><button class="danger" @click.stop="$emit('delete')">删除</button></footer>
  </article>
</template>
<style scoped>
.address-card{display:grid;gap:12px;padding:16px;border:3px solid var(--c-wood);background:var(--c-surface);box-shadow:var(--shadow-pixel)}.address-card.selectable{cursor:pointer}.address-card.selected{border-color:var(--c-moss);box-shadow:0 0 0 2px var(--c-amber),var(--shadow-pixel)}.address-card:focus-visible{outline:2px solid var(--c-amber);outline-offset:2px}.address-card header{display:flex;align-items:center;gap:10px}.address-card header span,.address-card p{color:var(--c-ink-2)}.address-card footer{display:flex;gap:12px;border-top:1px solid var(--c-line);padding-top:10px}.address-card button{border:0;background:transparent;color:var(--c-moss-dark);cursor:pointer}.address-card button.danger{margin-left:auto;color:var(--c-brick)}
</style>
