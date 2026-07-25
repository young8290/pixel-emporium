<script setup>
import { ref, reactive, computed } from 'vue'
import { list${RefDomain} } from '@/api/${module}/${refDomain}.js'

defineProps({
  modelValue: {
    type: [String, Number],
    default: null
  },
  placeholder: {
    type: String,
    default: '按名称搜索${refTableNameCn}'
  }
})

const emit = defineEmits(['update:modelValue'])

// ${refTableNameCn}选项数据
const ${refDomain}Options = ref([])
const loading = ref(false)

// 分页数据
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 当前搜索关键字
const currentKeyword = ref('')

const handleSearch = async (query) => {
  currentKeyword.value = query
  pagination.page = 1  // 重置页码
  await search${RefDomain}s()
}

// 搜索${refTableNameCn}
const search${RefDomain}s = async () => {
  if (!currentKeyword.value) {
    ${refDomain}Options.value = []
    return
  }

  loading.value = true
  try {
    const res = await list${RefDomain}({
      page: pagination.page,
      size: pagination.size,
      keyword: currentKeyword.value
    })
    if (res.code === 200) {
      const newOptions = res.data.list.map(item => ({
        value: item.id,
        label: item.${refLabelField},
      }))

      // 第一页替换，其他页追加
      if (pagination.page === 1) {
        ${refDomain}Options.value = newOptions
      } else {
        ${refDomain}Options.value = [...${refDomain}Options.value, ...newOptions]
      }

      pagination.total = res.data.total
    }
  } catch (error) {
    console.error('获取${refTableNameCn}列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const handleLoadMore = async (e) => {
  e.stopPropagation() // 阻止事件冒泡，防止关闭下拉框
  pagination.page++
  await search${RefDomain}s()
}

// 计算是否还有更多数据
const hasMore = computed(() => {
  return ${refDomain}Options.value.length < pagination.total
})
</script>

<template>
  <el-select
    :model-value="modelValue"
    filterable
    remote
    reserve-keyword
    :placeholder="placeholder"
    :remote-method="handleSearch"
    :loading="loading"
    @update:modelValue="newValue => emit('update:modelValue', newValue)"
    style="width: 100%"
  >
    <el-option
      v-for="item in ${refDomain}Options"
      :key="item.value"
      :label="item.label"
      :value="item.value"
    />
    <div v-if="hasMore" class="load-more">
      <el-button
        link
        type="primary"
        size="small"
        :loading="loading"
        @click="handleLoadMore"
      >
        加载更多
      </el-button>
    </div>
  </el-select>
</template>

<style scoped>
.load-more {
  padding: 5px 0;
  text-align: center;
  border-top: 1px solid #EBEEF5;
  cursor: pointer;
}
</style>
