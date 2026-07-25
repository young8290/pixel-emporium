<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listBusinessOperationLogs, listUserOperationLogs } from '@/api/operationLog'
import FilterBar from '@/components/business/FilterBar.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const source = ref('business')
const query = reactive({ page: 1, size: 20, userId: null, action: '', targetType: '' })

async function load() {
  loading.value = true
  try {
    const listOperationLogs = source.value === 'user'
      ? listUserOperationLogs
      : listBusinessOperationLogs
    const data = await listOperationLogs(query)
    rows.value = data?.records || data?.list || []
    total.value = Number(data?.total || rows.value.length)
  } finally {
    loading.value = false
  }
}

function changeSource() {
  query.page = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header>
      <small>AUDIT TRAIL</small>
      <h1>操作日志</h1>
    </header>
    <FilterBar>
      <el-select v-model="source" aria-label="日志来源" @change="changeSource">
        <el-option label="商城操作" value="business" />
        <el-option label="用户与邮件操作" value="user" />
      </el-select>
      <el-input v-model="query.userId" placeholder="操作人 ID" />
      <el-input v-model="query.action" placeholder="动作" />
      <el-input v-model="query.targetType" placeholder="对象类型" />
      <template #actions>
        <el-button @click="load">筛选</el-button>
      </template>
    </FilterBar>
    <section class="table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无审计日志">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="userId" label="操作人" width="90" />
        <el-table-column prop="action" label="动作" width="190" />
        <el-table-column prop="targetType" label="对象" width="150" />
        <el-table-column prop="targetId" label="对象 ID" width="120" />
        <el-table-column prop="ip" label="来源 IP" min-width="140" />
        <el-table-column prop="result" label="结果" width="100" />
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        @current-change="load"
      />
    </section>
  </div>
</template>

<style scoped>
.page { display: grid; gap: 18px; }
header small { color: var(--c-moss-dark); }
h1 { font-family: var(--font-pixel); color: var(--c-wood-dark); }
.table-card { padding: 14px; border: 3px solid var(--c-wood-dark); background: var(--c-surface); box-shadow: var(--shadow-pixel); overflow: auto; }
</style>
