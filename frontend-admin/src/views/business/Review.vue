<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { hideReview, listReviews, restoreReview } from '@/api/business/review'
import FilterBar from '@/components/business/FilterBar.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drawerOpen = ref(false)
const current = ref(null)
const hideOpen = ref(false)
const hideTarget = ref(null)
const hideReason = ref('')
const reasonError = ref('')
const submitting = ref(false)
const query = reactive({
  page: 1,
  size: 10,
  productId: null,
  userId: null,
  rating: null,
  status: '',
  keyword: '',
  createdAtStart: '',
  createdAtEnd: ''
})

async function load() {
  loading.value = true
  try {
    const data = await listReviews({ ...query })
    rows.value = data?.records || data?.list || []
    total.value = Number(data?.total || rows.value.length)
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  query.page = 1
  return load()
}

function changePage(page) {
  query.page = page
  return load()
}

function openDetails(row) {
  current.value = row
  drawerOpen.value = true
}

function openHide(row) {
  hideTarget.value = row
  hideReason.value = ''
  reasonError.value = ''
  hideOpen.value = true
}

async function submitHide() {
  const reason = hideReason.value.trim()
  if (reason.length < 2 || reason.length > 200) {
    reasonError.value = '隐藏原因需为 2 到 200 个字符'
    return
  }
  reasonError.value = ''
  await ElMessageBox.confirm('隐藏后该评价不会出现在商品详情中，确定继续？', '隐藏评价', { type: 'warning' })
  submitting.value = true
  try {
    await hideReview(hideTarget.value.id, reason)
    hideOpen.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

async function restore(row) {
  await ElMessageBox.confirm('恢复后该评价会重新计入商品评分，确定继续？', '恢复评价', { type: 'warning' })
  await restoreReview(row.id)
  await load()
}

function statusType(status) {
  return status === 'PUBLISHED' ? 'success' : 'danger'
}

function statusLabel(status) {
  return status === 'PUBLISHED' ? '已发布' : '已隐藏'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header>
      <small>PRODUCT REVIEWS</small>
      <h1>评论管理</h1>
    </header>

    <FilterBar>
      <el-input v-model="query.keyword" clearable placeholder="商品或评论关键词" />
      <el-input v-model="query.productId" clearable placeholder="商品 ID" />
      <el-input v-model="query.userId" clearable placeholder="用户 ID" />
      <el-select v-model="query.rating" clearable placeholder="评分">
        <el-option v-for="score in 5" :key="score" :label="`${score} 星`" :value="score" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="状态">
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已隐藏" value="HIDDEN" />
      </el-select>
      <el-date-picker
        v-model="query.createdAtStart"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="开始时间"
      />
      <el-date-picker
        v-model="query.createdAtEnd"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="结束时间"
      />
      <template #actions>
        <el-button @click="applyFilters">筛选</el-button>
      </template>
    </FilterBar>

    <section class="table-card">
      <el-table v-loading="loading" :data="rows" empty-text="暂无商品评价">
        <el-table-column prop="id" label="ID" width="84" />
        <el-table-column prop="productTitle" label="商品" min-width="170" />
        <el-table-column label="评分" width="88">
          <template #default="{ row }"><span class="rating">★ {{ row.rating }}</span></template>
        </el-table-column>
        <el-table-column label="评论" min-width="240">
          <template #default="{ row }"><p class="content-preview">{{ row.content }}</p></template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评价时间" width="178" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openDetails(row)">详情</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="danger" @click="openHide(row)">隐藏</el-button>
            <el-button v-else link type="success" @click="restore(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        @current-change="changePage"
      />
    </section>

    <el-drawer v-model="drawerOpen" title="评价详情" size="min(520px, 100vw)">
      <div v-if="current" class="review-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品">{{ current.productTitle }}</el-descriptions-item>
          <el-descriptions-item label="SKU">{{ current.skuTitle || '默认规格' }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单 ID">{{ current.orderId }}</el-descriptions-item>
          <el-descriptions-item label="评分">★ {{ current.rating }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(current.status) }}</el-descriptions-item>
          <el-descriptions-item label="评价时间">{{ current.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <h2>评论正文</h2>
        <p data-testid="review-content" class="full-content">{{ current.content }}</p>
        <template v-if="current.status === 'HIDDEN'">
          <h2>隐藏原因</h2>
          <p class="moderation-reason">{{ current.moderationReason }}</p>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="hideOpen" title="隐藏评价" width="min(520px, calc(100vw - 24px))">
      <p class="dialog-hint">请输入审核原因，用户可在订单详情中看到该说明。</p>
      <el-input
        v-model="hideReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="例如：评论包含与商品无关的内容"
      />
      <p v-if="reasonError" class="form-error">{{ reasonError }}</p>
      <template #footer>
        <el-button @click="hideOpen = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitHide">确认隐藏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page{display:grid;gap:18px}
header small{color:var(--c-moss-dark)}
h1{font-family:var(--font-pixel);color:var(--c-wood-dark)}
.table-card{padding:14px;border:3px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel);overflow:auto}
.rating{color:var(--c-gold-dark);white-space:nowrap}
.content-preview{display:-webkit-box;margin:0;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:2;line-height:1.5;word-break:break-word}
.review-detail{display:grid;gap:14px}
.review-detail h2{margin:4px 0 0;font-family:var(--font-pixel);font-size:16px;color:var(--c-wood-dark)}
.full-content,.moderation-reason{margin:0;padding:12px;border:2px solid var(--c-wood);background:var(--c-paper);line-height:1.7;white-space:pre-wrap;word-break:break-word}
.moderation-reason{color:var(--c-brick)}
.dialog-hint{margin-top:0;color:var(--c-text-muted)}
.form-error{margin:8px 0 0;color:var(--c-brick)}
</style>
