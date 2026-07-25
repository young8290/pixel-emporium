<script setup>import {useRouter} from 'vue-router';import PixelErrorState from '@/components/ui/PixelErrorState.vue';import image from '@/assets/pixel/illustrations/error-404.png';const router=useRouter()</script>
<template><PixelErrorState :image="image" title="这块田还没开垦" desc="你访问的页面不存在或已经搬走。" retry="返回首页" @retry="router.push('/')"/></template>
