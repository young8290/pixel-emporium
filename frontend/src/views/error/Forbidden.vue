<script setup>import {useRouter} from 'vue-router';import PixelErrorState from '@/components/ui/PixelErrorState.vue';import image from '@/assets/pixel/illustrations/error-403.png';const router=useRouter()</script>
<template><PixelErrorState :image="image" title="这扇门暂不开放" desc="当前账号没有访问权限。" retry="返回上一页" @retry="router.back()"/></template>
