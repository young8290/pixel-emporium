<script setup>
import { onMounted, ref } from 'vue'
import { deleteAddress, listAddresses, saveAddress, setDefaultAddress } from '@/api/address'
import AddressCard from '@/components/business/AddressCard.vue'
import AddressForm from '@/components/business/AddressForm.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelConfirmDialog from '@/components/ui/PixelConfirmDialog.vue'
import PixelDialog from '@/components/ui/PixelDialog.vue'
import PixelEmptyState from '@/components/ui/PixelEmptyState.vue'
import PixelErrorState from '@/components/ui/PixelErrorState.vue'
import PixelSkeleton from '@/components/ui/PixelSkeleton.vue'
import empty from '@/assets/pixel/empty-states/empty-address.png'
import network from '@/assets/pixel/illustrations/error-network.png'
const addresses=ref([]),loading=ref(true),failed=ref(false),saving=ref(false),dialog=ref(false),editing=ref({}),removing=ref(null),deleting=ref(false)
async function load(){loading.value=true;failed.value=false;try{const data=await listAddresses();addresses.value=data?.records||data?.list||data||[]}catch{failed.value=true}finally{loading.value=false}}
function open(item={}){editing.value={...item};dialog.value=true}
async function save(payload){saving.value=true;try{await saveAddress(payload);dialog.value=false;await load()}finally{saving.value=false}}
async function setDefault(item){await setDefaultAddress(item.id);await load()}
async function remove(){deleting.value=true;try{await deleteAddress(removing.value.id);removing.value=null;await load()}finally{deleting.value=false}}
onMounted(load)
</script>
<template><div class="address-page"><header><div><small>ADDRESS BOOK</small><h1>收货地址</h1></div><PixelButton @click="open()">新增地址</PixelButton></header><PixelSkeleton v-if="loading" :rows="5"/><PixelErrorState v-else-if="failed" :image="network" @retry="load"/><PixelEmptyState v-else-if="!addresses.length" :image="empty" title="地址簿还是空的" cta="新增地址" @cta-click="open()"/><section v-else class="grid"><AddressCard v-for="item in addresses" :key="item.id" :address="item" @edit="open(item)" @delete="removing=item" @set-default="setDefault(item)"/></section><PixelDialog v-model="dialog" :title="editing.id?'编辑地址':'新增地址'" @cancel="dialog=false"><AddressForm :model-value="editing" :loading="saving" @submit="save"/></PixelDialog><PixelConfirmDialog :model-value="!!removing" title="删除地址" content="删除后无法恢复，确定继续吗？" danger :loading="deleting" @update:model-value="!$event&&(removing=null)" @cancel="removing=null" @confirm="remove"/></div></template>
<style scoped>.address-page{display:grid;gap:var(--sp-4)}header{display:flex;align-items:center;justify-content:space-between}header small{color:var(--c-moss-dark)}h1{font-family:var(--font-pixel);font-size:30px;color:var(--c-wood-dark)}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-4)}@media(max-width:680px){.grid{grid-template-columns:1fr}}</style>
