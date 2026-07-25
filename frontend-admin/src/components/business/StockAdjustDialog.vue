<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { saveSku } from '@/api/business/sku'

const emit = defineEmits(['saved'])
const open = ref(false)
const saving = ref(false)
const sku = ref(null)
const form = reactive({ mode: 'delta', delta: 0, target: 0, reason: '' })
const nextStock = computed(() => form.mode === 'delta'
  ? Number(sku.value?.stock || 0) + Number(form.delta || 0)
  : Number(form.target || 0))

function show(row) {
  sku.value = row
  Object.assign(form, { mode: 'delta', delta: 0, target: Number(row.stock || 0), reason: '' })
  open.value = true
}

async function submit() {
  if (nextStock.value < 0) {
    ElMessage.warning('调整后库存不能小于 0')
    return
  }
  if (!form.reason.trim()) {
    ElMessage.warning('请填写库存调整原因')
    return
  }
  saving.value = true
  try {
    await saveSku({
      id: sku.value.id,
      title: sku.value.title,
      description: sku.value.description || '',
      price: sku.value.price,
      stock: nextStock.value,
      version: sku.value.version,
      productId: sku.value.productId,
      skuCode: sku.value.skuCode || '',
      mainImageId: sku.value.mainImageId,
      attributeValueIds: sku.value.attributeValueIds || [],
      stockAdjustReason: form.reason.trim(),
    })
    ElMessage.success('库存调整成功，流水已记录')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ show })
</script>

<template>
  <el-dialog v-model="open" title="调整库存" width="520px">
    <dl class="stock-summary">
      <div><dt>SKU</dt><dd>{{ sku?.title }}</dd></div>
      <div><dt>当前库存</dt><dd>{{ sku?.stock }}</dd></div>
      <div><dt>调整后</dt><dd :class="{ danger: nextStock < 0 }">{{ nextStock }}</dd></div>
    </dl>
    <el-form label-width="96px">
      <el-form-item label="调整方式">
        <el-radio-group v-model="form.mode">
          <el-radio-button value="delta">增减数量</el-radio-button>
          <el-radio-button value="target">设为库存</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="form.mode === 'delta' ? '增减数量' : '目标库存'">
        <el-input-number
          v-if="form.mode === 'delta'"
          v-model="form.delta"
          :min="-Number(sku?.stock || 0)"
        />
        <el-input-number v-else v-model="form.target" :min="0" />
      </el-form-item>
      <el-form-item label="调整原因" required>
        <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="open = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认调整</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stock-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-2);
  margin: 0 0 var(--sp-4);
}

.stock-summary div {
  padding: var(--sp-2);
  border: 2px solid var(--c-line);
  background: var(--c-surface-2);
}

dt { color: var(--c-ink-2); }
dd { margin: var(--sp-1) 0 0; font: 700 20px/1 var(--font-pixel); }
.danger { color: var(--c-brick); }
</style>
