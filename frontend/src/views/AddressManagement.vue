<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {listAddress, saveAddress, deleteAddress} from '@/api/user/address.js'
import {Refresh, Plus} from '@element-plus/icons-vue'

const addressList = ref([])
const tableLoading = ref(false)

// 分页相关数据
const pagination = reactive({
  // 每页显示的数据条数
  size: 3,
  // 当前页码，从1开始
  page: 1,
  // 总数据条数，应该在获取数据时更新
  total: 0
})

// 分页变化处理函数
const handleCurrentChange = async (val) => {
  pagination.page = val
  await getAddressList()
}

const handleSizeChange = async (val) => {
  pagination.size = val
  pagination.page = 1
  await getAddressList()
}

const handleRefresh = () => {
  getAddressList()
}

// 搜索表单
const searchForm = reactive({
  keyword: ''  // 搜索关键字
})

// 处理搜索
const handleSearch = () => {
  pagination.page = 1  // 重置到第一页
  getAddressList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

// 获取收货地址列表数据
const getAddressList = async () => {
  tableLoading.value = true
  try {
    const res = await listAddress({
      ...pagination,
      keyword: searchForm.keyword
    })
    if (res.code === 200) {
      addressList.value = res.data.list
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.msg || '获取收货地址列表失败')
    }
  } catch (error) {
    console.error('获取收货地址列表失败:', error)
    ElMessage.error('获取收货地址列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 在组件挂载时获取数据
onMounted(() => {
  getAddressList()
})

// 控制对话框的显示
const dialogVisible = ref(false)

const dialogTitle = computed(() => {
  return addressForm.id ? '编辑收货地址' : '新增收货地址'
})

// 收货地址表单数据
const addressForm = reactive({
  id: null,
  address: null,
  userId: 0,
})

// 表单规则
const rules = {
  address: [
    {required: true, message: '请输入地址', trigger: 'blur'},
  ],
}

const formRef = ref(null)

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  addressForm.id = null
  addressForm.address = null
  addressForm.userId = 0
}

const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    const res = await saveAddress(addressForm)
    if (res.code === 200) {
      ElMessage.success(addressForm.id ? '编辑成功' : '添加成功')
      dialogVisible.value = false
      resetForm()
      // 刷新乘客列表
      pagination.page = 1
      await getAddressList()
    } else {
      // 场景：http 返回头的状态码是 200，但是返回体中的 code 字段值不是 200
      ElMessage.error(res.msg || (addressForm.id ? '编辑收货地址失败' : '添加收货地址失败'))
    }
  } catch (error) {
    ElMessage.error(error.response.data.msg || (addressForm.id ? '编辑收货地址失败' : '添加收货地址失败'))
  }
}

const handleEdit = (row) => {
  Object.assign(addressForm, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除收货地址 ' + row.id + ' 吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteAddress(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        // 如果当前页只有一条数据且不是第一页，删除后自动跳转到上一页
        if (addressList.value.length === 1 && pagination.page > 1) {
          pagination.page--
        }
        await getAddressList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除收货地址失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除，不做任何操作
    console.log('取消删除，不做任何操作')
  })
}
</script>

<template>
  <div class="address-container">
    <!-- 顶部按钮 -->
    <div class="top-tools">
      <div class="left">
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增收货地址</el-button>
      </div>
      <div class="right">
        <el-form :inline="true" :model="searchForm" @submit.prevent>
          <el-form-item class="mb0">
            <el-input
              v-model="searchForm.keyword"
              placeholder="请输入关键词"
              clearable
            />
          </el-form-item>
          <el-form-item class="mb0">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-table
      :data="addressList"
      v-loading="tableLoading"
      style="width: 100%"
      border
    >
      <el-table-column prop="id" label="编号" width="180"/>
      <el-table-column prop="userId" label="所属用户ID" width="180"/>
      <el-table-column prop="address" label="地址"/>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

    <!-- 收货地址对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="addressForm"
        :rules="rules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="addressForm.address"
            placeholder="请输入地址"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.address-container {
  padding: 20px;
}

:deep(.el-scrollbar) {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.top-tools {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.top-tools .left {
  display: flex;
  gap: 10px;
}

.top-tools .right {
  display: flex;
  align-items: center;
  margin-left: auto;

  .el-form .el-form-item:last-child {
    margin-right: 0;
  }
}

@media screen and (max-width: 768px) {
  .top-tools {
    flex-direction: column;
    align-items: flex-start;
  }

  .top-tools .right {
    margin-left: 0;
    width: 100%;
  }
}

.mb0 {
  margin-bottom: 0;
}
</style>
