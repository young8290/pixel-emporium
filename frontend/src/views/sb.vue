<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listSb } from '@/api/user/sb.js'

const sbList = ref([])
const tableLoading = ref(false)

const pagination = reactive({
  page: 1,
  size: 3,
  total: 0
})

const getSbList = async () => {
  try {
    tableLoading.value = true

    const res = await listSb(pagination)

    if (res.code === 200) {
      sbList.value = res.data.list
      pagination.total = res.data.total
      return
    }

    ElMessage.error(res.msg || '获取 SB 列表失败')
  } catch (error) {
    console.error('获取 SB 列表失败:', error)
    ElMessage.error('获取 SB 列表失败')
  } finally {
    tableLoading.value = false
  }
}

const handleCurrentChange = async (page) => {
  pagination.page = page
  await getSbList()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await getSbList()
}

const handleRefresh = async () => {
  await getSbList()
}

onMounted(() => {
  getSbList()
})
</script>

<template>
  <div class="top-tools">
    <div class="left">
      <el-button :icon="Refresh" @click="handleRefresh">
        刷新
      </el-button>
    </div>
  </div>

  <div class="sb-container">
    <el-table
      v-loading="tableLoading"
      :data="sbList"
      style="width: 100%"
      border
    >
      <el-table-column prop="id" label="编号" />
      <el-table-column prop="userId" label="所属用户 ID" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="address" label="地址" />
      <el-table-column
        prop="introduction"
        label="简介"
        show-overflow-tooltip
      />
    </el-table>
  </div>

  <div class="pagination-container">
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :page-sizes="[2, 5, 10, 20]"
      :total="pagination.total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<style scoped>
.sb-container {
  padding: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
