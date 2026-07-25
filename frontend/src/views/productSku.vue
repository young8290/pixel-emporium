<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {Plus, Refresh} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {saveProductSku, deleteProductSku, listProductSku} from '@/api/business/productSku.js'
import {listProduct} from '@/api/business/product.js'

// SKU 列表数据
const skuList = ref([])

// 控制对话框的显示
const dialogVisible = ref(false)

// SKU 表单数据
const skuForm = reactive({
  id: null,
  title: null,
  description: null,
  price: 0.00,
  stock: 0,
  productId: null,
})

// 表单规则
const rules = {
  title: [
    {required: true, message: '请输入名称', trigger: 'blur'},
  ],
  description: [
    {required: false, message: '请输入描述', trigger: 'blur'},
  ],
  price: [
    {required: true, message: '请输入价格', trigger: 'blur'},
  ],
  stock: [
    {required: true, message: '请输入库存', trigger: 'blur'},
  ],
  productId: [
    {required: true, message: '请选择所属商品', trigger: 'change'},
  ],
}

const formRef = ref(null)

// 商品下拉选项（功能1）
const productOptions = ref([])
const productLoading = ref(false)

// 加载商品下拉选项
const loadProductOptions = async (keyword = '') => {
  productLoading.value = true
  try {
    const res = await listProduct({page: 1, size: 50, keyword})
    if (res.code === 200) {
      productOptions.value = res.data.list
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    productLoading.value = false
  }
}

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  skuForm.id = null
  skuForm.title = null
  skuForm.description = null
  skuForm.price = 0.00
  skuForm.stock = 0
  skuForm.productId = null
}

const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    const res = await saveProductSku(skuForm)
    if (res.code === 200) {
      ElMessage.success(skuForm.id ? '编辑成功' : '添加成功')
      dialogVisible.value = false
      resetForm()
      pagination.page = 1
      await getSkuList()
    } else {
      ElMessage.error(res.msg || (skuForm.id ? '编辑SKU失败' : '添加SKU失败'))
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || (skuForm.id ? '编辑SKU失败' : '添加SKU失败'))
  }
}

const handleEdit = (row) => {
  Object.assign(skuForm, row)
  // 确保当前所属商品在下拉选项中，避免只显示商品ID
  if (row.productId && !productOptions.value.some(item => item.id === row.productId)) {
    productOptions.value.unshift({id: row.productId, title: row.productTitle})
  }
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除SKU ' + row.title + ' 吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteProductSku(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        if (skuList.value.length === 1 && pagination.page > 1) {
          pagination.page--
        }
        await getSkuList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除SKU失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    console.log('取消删除，不做任何操作')
  })
}

const dialogTitle = computed(() => {
  return skuForm.id ? '编辑SKU' : '新增SKU'
})

const tableLoading = ref(false)

// 获取SKU列表数据
const getSkuList = async () => {
  tableLoading.value = true
  try {
    const res = await listProductSku({
      ...pagination,
      keyword: searchForm.keyword
    })
    if (res.code === 200) {
      skuList.value = res.data.list
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.msg || '获取SKU列表失败')
    }
  } catch (error) {
    console.error('获取SKU列表失败:', error)
    ElMessage.error('获取SKU列表失败')
  } finally {
    tableLoading.value = false
  }
}

onMounted(() => {
  getSkuList()
  loadProductOptions()
})

// 分页相关数据
const pagination = reactive({
  size: 10,
  page: 1,
  total: 0
})

const handleCurrentChange = async (val) => {
  pagination.page = val
  await getSkuList()
}

const handleSizeChange = async (val) => {
  pagination.size = val
  pagination.page = 1
  await getSkuList()
}

const handleRefresh = () => {
  getSkuList()
}

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

const handleSearch = () => {
  pagination.page = 1
  getSkuList()
}

const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}
</script>

<template>
  <div class="sku-container">
    <!-- 顶部按钮 -->
    <div class="top-tools">
      <div class="left">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增SKU</el-button>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
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

    <!-- SKU列表 -->
    <el-table
      :data="skuList"
      v-loading="tableLoading"
      style="width: 100%"
      border
    >
      <el-table-column prop="title" label="名称"/>
      <el-table-column prop="description" label="描述"/>
      <el-table-column prop="price" label="价格"/>
      <el-table-column prop="stock" label="库存"/>
      <el-table-column prop="productId" label="所属商品"/>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
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

    <!-- SKU对话框 -->
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
        :model="skuForm"
        :rules="rules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="名称" prop="title">
          <el-input
            v-model="skuForm.title"
            placeholder="请输入名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="skuForm.description"
            placeholder="请输入描述"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
          />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="skuForm.price" :precision="2" :min="0" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="skuForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="所属商品" prop="productId">
          <el-select
            v-model="skuForm.productId"
            placeholder="请选择所属商品"
            filterable
            remote
            :remote-method="loadProductOptions"
            :loading="productLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
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
.sku-container {
  padding: 20px;
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

.el-form--inline .el-form-item {
  margin-right: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-scrollbar) {
  margin-bottom: 0;
}
</style>
