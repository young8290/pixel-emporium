<script setup>defineProps({status:String});const labels={PENDING_PAYMENT:'待付款',PAID:'待发货',SHIPPED:'待收货',COMPLETED:'已完成',CANCELED:'已取消'};const kinds={PENDING_PAYMENT:'warning',PAID:'success',SHIPPED:'primary',COMPLETED:'success',CANCELED:'info'}</script>
<template><el-tag :type="kinds[status]||'info'" effect="dark">{{ labels[status]||status||'—' }}</el-tag></template>
