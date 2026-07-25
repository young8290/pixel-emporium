<script setup>
import CustomMarkdown from '@/components/CustomMarkdown.vue'
import ImageUploader from '@/components/ImageUploader.vue'

defineProps({
  form: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
})
</script>

<template>
  <el-form label-width="90px">
    <el-form-item label="标题" required>
      <el-input v-model="form.title" maxlength="120" show-word-limit />
    </el-form-item>
    <el-form-item label="分类" required>
      <el-tree-select
        v-model="form.categoryId"
        :data="categories"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        check-strictly
      />
    </el-form-item>
    <el-form-item label="价格" required>
      <el-input-number v-model="form.price" :min=".01" :precision="2" />
    </el-form-item>
    <el-form-item label="封面">
      <ImageUploader v-model="form.image" />
    </el-form-item>
    <el-form-item label="商品详情">
      <CustomMarkdown
        :id="`product-description-${form.id || 'new'}`"
        v-model:markdown-text="form.description"
        height="320px"
      />
    </el-form-item>
    <el-form-item label="状态">
      <el-switch v-model="form.onSale" active-text="上架" inactive-text="下架" />
    </el-form-item>
  </el-form>
</template>

<style scoped>
:deep(.vditor) {
  width: 100%;
  border: 2px solid var(--c-line);
  border-radius: var(--r-sm);
}
</style>
