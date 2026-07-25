<script setup>defineProps({stock:{type:Number,default:0}})</script>
<template><p :class="['stock-hint',{danger:stock===0,warning:stock>0&&stock<=5}]">{{ stock===0?'暂时售罄':stock<=5?`仅剩 ${stock} 件`:`库存充足 · ${stock} 件` }}</p></template>
<style scoped>.stock-hint{color:var(--c-moss-dark)}.stock-hint.warning{color:var(--c-pumpkin)}.stock-hint.danger{color:var(--c-brick)}</style>
