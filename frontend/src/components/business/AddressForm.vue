<script setup>
import { reactive, watch } from 'vue'
import PixelInput from '@/components/ui/PixelInput.vue'
import PixelCheckbox from '@/components/ui/PixelCheckbox.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
const props=defineProps({modelValue:{type:Object,default:()=>({})},loading:Boolean})
const emit=defineEmits(['submit'])
const form=reactive({})
watch(()=>props.modelValue,(v)=>Object.assign(form,{id:null,recipient:'',phone:'',province:'',city:'',district:'',detail:'',isDefault:false,...v}),{immediate:true,deep:true})
function submit(){if(!form.recipient||!/^1\d{10}$/.test(form.phone)||!form.province||!form.city||!form.district||!form.detail)return;emit('submit',{...form,address:[form.province,form.city,form.district,form.detail].join(' ')})}
</script>
<template><form class="address-form" @submit.prevent="submit"><label>收件人<PixelInput v-model="form.recipient" placeholder="姓名" :error="!form.recipient?'请填写收件人':''" /></label><label>手机号<PixelInput v-model="form.phone" placeholder="11 位手机号" :error="form.phone&&!/^1\d{10}$/.test(form.phone)?'手机号格式不正确':''" /></label><div class="region"><label>省<PixelInput v-model="form.province" /></label><label>市<PixelInput v-model="form.city" /></label><label>区/县<PixelInput v-model="form.district" /></label></div><label>详细地址<PixelInput v-model="form.detail" placeholder="街道、门牌号" /></label><PixelCheckbox v-model="form.isDefault">设为默认地址</PixelCheckbox><PixelButton native-type="submit" :loading="loading" block>保存地址</PixelButton></form></template>
<style scoped>.address-form{display:grid;gap:14px}.address-form label{display:grid;gap:6px;color:var(--c-ink-2)}.region{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}@media(max-width:520px){.region{grid-template-columns:1fr}}</style>
