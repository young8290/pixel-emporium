<script setup>
import QtyStepper from './QtyStepper.vue'
import PixelCheckbox from '@/components/ui/PixelCheckbox.vue'
import PixelTag from '@/components/ui/PixelTag.vue'
import placeholder from '@/assets/pixel/products/product-placeholder.png'
import trash from '@/assets/pixel/icons/icon-delete.png'
defineProps({item:{type:Object,required:true},busy:Boolean})
defineEmits(['update','remove','check'])
</script>
<template>
  <article class="cart-item" :class="{invalid:item.invalid}">
    <PixelCheckbox :model-value="item.checked" :disabled="item.invalid||busy" aria-label="选择商品" @update:model-value="$emit('check',$event)" />
    <img class="photo" :src="item.productImage||placeholder" :alt="item.productTitle" data-pixel>
    <div class="info"><h3>{{ item.productTitle }}</h3><p>{{ item.skuTitle }}</p><PixelTag v-if="item.invalid" text="失效" color="brick" /></div>
    <strong>¥{{ Number(item.price||0).toFixed(2) }}</strong>
    <QtyStepper :model-value="item.quantity" :max="item.stock||1" :disabled="item.invalid||busy" @change="$emit('update',$event)" />
    <b>¥{{ (Number(item.price||0)*Number(item.quantity||0)).toFixed(2) }}</b>
    <button type="button" aria-label="删除" :disabled="busy" @click="$emit('remove')"><img :src="trash" alt="" data-pixel></button>
  </article>
</template>
<style scoped>
.cart-item{display:grid;grid-template-columns:28px 92px minmax(140px,1fr) 90px 134px 100px 42px;align-items:center;gap:12px;padding:14px;border-bottom:2px solid var(--c-line);background:var(--c-surface)}.cart-item.invalid{opacity:.58}.photo{width:88px;height:88px;border:2px solid var(--c-wood);object-fit:cover}.info{display:grid;gap:5px}.info h3{font-size:15px}.info p{color:var(--c-ink-2);font-size:12px}.cart-item>strong,.cart-item>b{color:var(--c-amber)}.cart-item>button{display:grid;place-items:center;width:40px;height:40px;border:2px solid var(--c-wood);background:var(--c-surface);cursor:pointer}.cart-item>button img{width:22px}@media(max-width:800px){.cart-item{grid-template-columns:28px 76px 1fr 42px}.photo{width:72px;height:72px}.cart-item>strong{display:none}.cart-item :deep(.stepper){grid-column:2/4}.cart-item>b{grid-column:3}.cart-item>button{grid-column:4;grid-row:1/3}}
</style>
