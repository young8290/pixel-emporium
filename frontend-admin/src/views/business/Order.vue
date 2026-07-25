<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listOrders } from '@/api/business/order'
import FilterBar from '@/components/business/FilterBar.vue'
import StatusTag from '@/components/business/StatusTag.vue'
const router=useRouter(),rows=ref([]),total=ref(0),loading=ref(false)
const query=reactive({page:1,size:10,orderNo:'',userId:null,status:null,createdAtStart:null,createdAtEnd:null})
const dates=ref([])
async function load(){loading.value=true;try{const params={...query,createdAtStart:dates.value?.[0],createdAtEnd:dates.value?.[1]};const data=await listOrders(params);rows.value=data?.records||data?.list||[];total.value=Number(data?.total||rows.value.length)}finally{loading.value=false}}
onMounted(load)
</script>
<template><div class="page"><header><small>ORDERS</small><h1>订单管理</h1></header><FilterBar><el-input v-model="query.orderNo" placeholder="订单号"/><el-input v-model="query.userId" placeholder="用户 ID"/><el-select v-model="query.status" clearable placeholder="状态"><el-option v-for="s in ['PENDING_PAYMENT','PAID','SHIPPED','COMPLETED','CANCELED']" :key="s" :label="s" :value="s"/></el-select><el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始" end-placeholder="结束"/><template #actions><el-button @click="load">筛选</el-button></template></FilterBar><section class="table-card"><el-table v-loading="loading" :data="rows"><el-table-column prop="orderNo" label="订单号" min-width="190"/><el-table-column prop="userId" label="用户" width="90"/><el-table-column prop="receiverName" label="收件人" width="110"/><el-table-column label="金额" width="110"><template #default="{row}">¥{{ Number(row.payAmount||0).toFixed(2) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{row}"><StatusTag :status="row.status"/></template></el-table-column><el-table-column prop="createdAt" label="下单时间" width="180"/><el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button link @click="router.push(`/order/${row.id}`)">详情</el-button><el-button v-if="row.status==='PAID'" link type="success" @click="router.push(`/order/${row.id}`)">去发货</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" @current-change="load"/></section></div></template>
<style scoped>.page{display:grid;gap:18px}header small{color:var(--c-moss-dark)}h1{font-family:var(--font-pixel);color:var(--c-wood-dark)}.table-card{padding:14px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel);overflow:auto}</style>
