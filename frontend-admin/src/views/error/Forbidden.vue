<script setup>import {useRouter} from 'vue-router';import image from '@/assets/pixel/illustrations/error-403.png';const router=useRouter()</script>
<template><div class="error"><img :src="image" alt="无访问权限" data-pixel><h1>没有工作台权限</h1><p>当前账号无法访问这个管理功能。</p><el-button @click="router.back()">返回</el-button></div></template>
