<script setup>import {useRouter} from 'vue-router';import image from '@/assets/pixel/illustrations/error-404.png';const router=useRouter()</script>
<template><div class="error"><img :src="image" alt="页面不存在" data-pixel><h1>这间仓库不存在</h1><p>请检查地址，或返回仪表盘。</p><el-button type="primary" @click="router.push('/dashboard')">返回仪表盘</el-button></div></template>
