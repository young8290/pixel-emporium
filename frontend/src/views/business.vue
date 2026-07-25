<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {Plus, Refresh, ZoomIn} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {saveProduct, deleteProduct, listProduct} from '@/api/business/product.js'
import ImageUploader from '@/components/ImageUploader.vue'

// 商品列表数据
const productList = ref([])

// 控制对话框的显示
const dialogVisible = ref(false)


// 商品表单数据
const productForm = reactive({
  id: null,
  title: null,
  description: null,
  image: null,
  onSale: 1,
  rating: 5.00,
  soldCount: 0,
  reviewCount: 0,
  price: 0.00,
})

// 表单规则
const rules = {
  title: [
    {required: true, message: '请输入名称', trigger: 'blur'},
  ],
  description: [
    {required: false, message: '请输入详情', trigger: 'blur'},
  ],
  image: [
    {required: false, message: '请输入封面图', trigger: 'blur'},
  ],
  onSale: [
    {required: true, message: '请输入上架状态', trigger: 'blur'},
  ],
  rating: [
    {required: false, message: '请输入平均评分', trigger: 'blur'},
  ],
  soldCount: [
    {required: false, message: '请输入销量', trigger: 'blur'},
  ],
  reviewCount: [
    {required: false, message: '请输入评价数量', trigger: 'blur'},
  ],
  price: [
    {required: true, message: '请输入最低价格', trigger: 'blur'},
  ],
}

const formRef = ref(null)

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  productForm.id = null
  productForm.title = null
  productForm.description = null
  productForm.image = null
  productForm.onSale = 1
  productForm.rating = 5.00
  productForm.soldCount = 0
  productForm.reviewCount = 0
  productForm.price = 0.00
}

const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    const res = await saveProduct(productForm)
    if (res.code === 200) {
      ElMessage.success(productForm.id ? '编辑成功' : '添加成功')
      dialogVisible.value = false
      resetForm()
      // 刷新乘客列表
      pagination.page = 1
      await getProductList()
    } else {
      // 场景：http 返回头的状态码是 200，但是返回体中的 code 字段值不是 200
      ElMessage.error(res.msg || (productForm.id ? '编辑商品失败' : '添加商品失败'))
    }
  } catch (error) {
    ElMessage.error(error.response.data.msg || (productForm.id ? '编辑商品失败' : '添加商品失败'))
  }
}

const handleEdit = (row) => {
  Object.assign(productForm, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除商品 ' + row.id + ' 吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteProduct(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        // 如果当前页只有一条数据且不是第一页，删除后自动跳转到上一页
        if (productList.value.length === 1 && pagination.page > 1) {
          pagination.page--
        }
        await getProductList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除商品失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除，不做任何操作
    console.log('取消删除，不做任何操作')
  })
}

const dialogTitle = computed(() => {
  return productForm.id ? '编辑商品' : '新增商品'
})

const tableLoading = ref(false)

// 获取商品列表数据
const getProductList = async () => {
  tableLoading.value = true
  try {
    const res = await listProduct({
      ...pagination,
      keyword: searchForm.keyword
    })
    if (res.code === 200) {
      productList.value = res.data.list
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.msg || '获取商品列表失败')
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 在组件挂载时获取数据
onMounted(() => {
  getProductList()
})

// 分页相关数据
const pagination = reactive({
  // 每页显示的数据条数
  size: 10,
  // 当前页码，从1开始
  page: 1,
  // 总数据条数，会在获取数据时更新
  total: 0
})

// 分页变化处理函数
const handleCurrentChange = async (val) => {
  pagination.page = val
  await getProductList()
}

const handleSizeChange = async (val) => {
  pagination.size = val
  pagination.page = 1
  await getProductList()
}

const handleRefresh = () => {
  getProductList()
}

// 搜索表单
const searchForm = reactive({
  keyword: ''  // 搜索关键字
})

// 处理搜索
const handleSearch = () => {
  pagination.page = 1  // 重置到第一页
  getProductList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

const previewVisible = ref(false)
const previewUrl = ref('')
const previewType = ref('') // 如：'image'

// 处理图片预览
const handlePreview = (url, type) => {
  if (!url) return
  previewUrl.value = url
  previewType.value = type
  previewVisible.value = true
}

</script>

<template>
  <div class="product-container">
    <!-- 顶部按钮 -->
    <div class="top-tools">
      <div class="left">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增商品</el-button>
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

    <!-- 商品列表 -->
    <el-table
      :data="productList"
      v-loading="tableLoading"
      style="width: 100%"
      border
    >
      <el-table-column prop="title" label="名称"/>
      <el-table-column prop="description" label="详情"/>
      <el-table-column prop="image" label="封面图">
        <template #default="{ row }">
          <div v-if="row.image" class="preview-cell">
            <el-image
              :src="row.image"
              class="preview-image"
              fit="cover"
            />
            <div class="preview-actions">
              <el-button
                type="primary"
                link
                @click.stop="handlePreview(row.image, 'image')"
              >
                <el-icon><ZoomIn /></el-icon>
              </el-button>
            </div>
          </div>
          <span v-else></span>
        </template>
      </el-table-column>
      <el-table-column prop="onSale" label="上架状态"/>
      <el-table-column prop="rating" label="平均评分"/>
      <el-table-column prop="soldCount" label="销量"/>
      <el-table-column prop="reviewCount" label="评价数量"/>
      <el-table-column prop="price" label="最低价格"/>
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

    <!-- 商品对话框 -->
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
        :model="productForm"
        :rules="rules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="名称" prop="title">
          <el-input
            v-model="productForm.title"
            placeholder="请输入名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="详情" prop="description">
          <el-input
            v-model="productForm.description"
            placeholder="请输入详情"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
          />
        </el-form-item>
        <el-form-item label="封面图" prop="image">
          <image-uploader v-model="productForm.image" />
        </el-form-item>
        <el-form-item label="上架状态" prop="onSale">
          <el-switch v-model="productForm.onSale" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="平均评分" prop="rating">
          <el-input-number v-model="productForm.rating" :precision="2" />
        </el-form-item>
        <el-form-item label="销量" prop="soldCount">
          <el-input-number v-model="productForm.soldCount" />
        </el-form-item>
        <el-form-item label="评价数量" prop="reviewCount">
          <el-input-number v-model="productForm.reviewCount" />
        </el-form-item>
        <el-form-item label="最低价格" prop="price">
          <el-input-number v-model="productForm.price" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="预览"
      width="800px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-image
        v-if="previewType === 'image'"
        :src="previewUrl"
        class="preview-dialog-image"
        fit="contain"
      />
    </el-dialog>

  </div>
</template>

<style scoped>
.product-container {
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
.preview-cell {
  position: relative;
  width: 100px;
  height: 60px;
  overflow: hidden;
  border-radius: 4px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-cell:hover .preview-actions {
  opacity: 1;
}

.preview-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-actions .el-button {
  color: white;
  font-size: 20px;
}

.preview-dialog-image {
  width: 100%;
  max-height: 600px;
}
</style>
