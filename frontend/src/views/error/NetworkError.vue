<script setup>import PixelErrorState from '@/components/ui/PixelErrorState.vue';import image from '@/assets/pixel/illustrations/error-network.png';defineEmits(['retry'])</script>
<template><PixelErrorState :image="image" title="去往集市的路断了" desc="请检查网络后再试一次。" @retry="$emit('retry')"/></template>
