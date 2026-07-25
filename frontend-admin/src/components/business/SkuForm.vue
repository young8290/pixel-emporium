<script setup>
defineProps({
  form: { type: Object, required: true },
  products: { type: Array, default: () => [] },
})
</script>

<template>
  <el-form label-width="90px">
    <el-form-item label="所属商品" required>
      <el-select v-model="form.productId" filterable>
        <el-option v-for="product in products" :key="product.id" :label="product.title" :value="product.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="标题" required>
      <el-input v-model="form.title" maxlength="255" show-word-limit />
    </el-form-item>
    <el-form-item label="SKU 编码">
      <el-input v-model="form.skuCode" maxlength="64" />
    </el-form-item>
    <el-form-item label="价格" required>
      <el-input-number v-model="form.price" :min=".01" :precision="2" />
    </el-form-item>
    <el-form-item label="原价（可选）">
      <el-input-number
        v-model="form.originalPrice"
        data-test="original-price"
        :min="Number(form.price || 0) + .01"
        :precision="2"
        placeholder="留空则不显示折扣"
      />
    </el-form-item>
    <el-form-item label="初始库存" required>
      <el-input-number v-model="form.stock" :min="0" :disabled="Boolean(form.id)" />
      <small v-if="form.id">编辑已有 SKU 时请使用独立的“调库存”操作。</small>
    </el-form-item>
  </el-form>
</template>

<style scoped>
small {
  display: block;
  margin-left: var(--sp-2);
  color: var(--c-ink-2);
}
</style>
