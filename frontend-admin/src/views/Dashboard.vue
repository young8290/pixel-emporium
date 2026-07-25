<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboardStatistics } from '@/api/dashboard'
import StatCard from '@/components/business/StatCard.vue'
import SimpleChart from '@/components/business/SimpleChart.vue'
import StatusTag from '@/components/business/StatusTag.vue'
import orderIcon from '@/assets/pixel/icons/icon-order.png'
import coinIcon from '@/assets/pixel/icons/icon-coin.png'
import boxIcon from '@/assets/pixel/icons/icon-box.png'

const loading = ref(true)
const failed = ref(false)
const data = ref({
  todayOrderCount: 0,
  todayPaidSales: 0,
  totalOrderCount: 0,
  onSaleProductCount: 0,
  sevenDayTrend: [],
  hotProducts: [],
  recentOrders: []
})
const chartValues = computed(() => data.value.sevenDayTrend.map(item => Number(item.orderCount || 0)))
const chartLabels = computed(() => data.value.sevenDayTrend.map(item => String(item.day || '').slice(5)))

async function load() {
  loading.value = true
  failed.value = false
  try {
    data.value = await getDashboardStatistics()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <header><small>FARM OVERVIEW</small><h1>经营仪表盘</h1><p>订单、销售额和热销商品均由数据库实时聚合。</p></header>
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-result v-else-if="failed" icon="error" title="数据加载失败"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <template v-else>
      <section class="stats">
        <StatCard label="今日订单" :value="data.todayOrderCount" :icon="orderIcon" :hint="`累计 ${data.totalOrderCount}`" />
        <StatCard label="今日已支付销售额" :value="`¥${Number(data.todayPaidSales || 0).toFixed(2)}`" :icon="coinIcon" />
        <StatCard label="在售商品" :value="data.onSaleProductCount" :icon="boxIcon" />
        <StatCard label="热销商品" :value="data.hotProducts.length" :icon="boxIcon" hint="展示销量前 5" />
      </section>
      <section class="grid">
        <SimpleChart :values="chartValues" :labels="chartLabels" />
        <article class="recent">
          <h2>近期订单</h2>
          <el-table :data="data.recentOrders" empty-text="暂无订单">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column prop="payAmount" label="金额" width="100"><template #default="{ row }">¥{{ Number(row.payAmount || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" /></template></el-table-column>
          </el-table>
        </article>
      </section>
      <article class="hot-products">
        <h2>热销商品</h2>
        <el-table :data="data.hotProducts" empty-text="暂无已支付商品">
          <el-table-column prop="productTitle" label="商品" min-width="220" />
          <el-table-column prop="soldQuantity" label="销量" width="100" />
          <el-table-column prop="paidAmount" label="成交额" width="130"><template #default="{ row }">¥{{ Number(row.paidAmount || 0).toFixed(2) }}</template></el-table-column>
        </el-table>
      </article>
    </template>
  </div>
</template>

<style scoped>
.dashboard { display: grid; gap: 20px; }
header small { color: var(--c-moss-dark); }
h1, h2 { font-family: var(--font-pixel); color: var(--c-wood-dark); }
header p { color: var(--c-ink-2); }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(460px, 1.3fr); gap: 18px; }
.recent, .hot-products { padding: 18px; border: 3px solid var(--c-wood-dark); background: var(--c-surface); box-shadow: var(--shadow-pixel); }
.recent h2, .hot-products h2 { margin-bottom: 14px; }
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); } .grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .stats { grid-template-columns: 1fr; } }
</style>
