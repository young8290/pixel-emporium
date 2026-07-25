<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import JSEncrypt from 'jsencrypt'
import { adminLogin } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import storefront from '@/assets/pixel/backgrounds/banner-home.png'
const route=useRoute(),router=useRouter(),user=useUserStore(),loading=ref(false),form=reactive({email:'',password:''})
const disabled=computed(()=>!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)||form.password.length<4)
async function submit(){if(disabled.value||loading.value)return;const encryptor=new JSEncrypt();encryptor.setPublicKey(import.meta.env.VITE_PUBLIC_KEY);const password=encryptor.encrypt(form.password);if(!password)return ElMessage.error('密码加密失败，请检查公钥配置');loading.value=true;try{const data=await adminLogin(form.email,password);user.setUserInfo(data);ElMessage.success('登录成功');router.replace(String(route.query.redirect||'/dashboard'))}finally{loading.value=false}}
</script>
<template><main class="login"><section class="art"><img :src="storefront" alt="像素百货门面" data-pixel></section><section class="card"><small>ADMIN WORKSHOP</small><h1>像素百货管理后台</h1><p>仅管理员与超级管理员可以进入。</p><el-form @submit.prevent="submit"><el-form-item><el-input v-model="form.email" type="email" placeholder="管理员邮箱" size="large"/></el-form-item><el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" size="large"/></el-form-item><el-button native-type="submit" type="primary" size="large" :disabled="disabled" :loading="loading">进入工作台</el-button></el-form></section></main></template>
<style scoped>.login{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(380px,.9fr);min-height:100vh}.art{display:grid;place-items:center;overflow:hidden;background:var(--c-moss-dark)}.art img{width:100%;height:100%;object-fit:contain}.card{display:grid;align-content:center;gap:12px;padding:clamp(28px,6vw,90px);background:var(--c-surface)}.card small{color:var(--c-moss-dark)}.card h1{font-family:var(--font-pixel);font-size:32px;color:var(--c-wood-dark)}.card p{color:var(--c-ink-2)}.card .el-form{margin-top:12px}.card .el-button{width:100%}@media(max-width:760px){.login{grid-template-columns:1fr}.art{display:none}.card{padding:24px}}</style>
