<script setup>
import { computed, ref, watch } from 'vue'
import { createReview, updateReview } from '@/api/review'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelDialog from '@/components/ui/PixelDialog.vue'
import PixelTextarea from '@/components/ui/PixelTextarea.vue'
import StarRating from './StarRating.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  eligibility: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])
const rating = ref(0)
const content = ref('')
const ratingError = ref('')
const contentError = ref('')
const saving = ref(false)
const editing = computed(() => Boolean(props.eligibility?.reviewId))

function reset() {
  rating.value = Number(props.eligibility?.rating) || 0
  content.value = props.eligibility?.content || ''
  ratingError.value = ''
  contentError.value = ''
}

function validate() {
  const trimmed = content.value.trim()
  ratingError.value = rating.value >= 1 && rating.value <= 5 ? '' : '请选择评分'
  contentError.value = trimmed.length >= 5 && trimmed.length <= 1000
    ? '' : '评价内容长度应为 5 到 1000 个字符'
  return !ratingError.value && !contentError.value
}

async function submit() {
  if (!validate() || saving.value) return
  saving.value = true
  const payload = { rating: rating.value, content: content.value.trim() }
  try {
    if (editing.value) {
      await updateReview(props.eligibility.reviewId, payload)
    } else {
      await createReview({ orderItemId: props.eligibility.orderItemId, ...payload })
    }
    emit('saved')
    emit('update:modelValue', false)
  } finally {
    saving.value = false
  }
}

watch(() => [props.modelValue, props.eligibility], ([open]) => {
  if (open) reset()
}, { immediate: true })
</script>

<template>
  <PixelDialog
    :model-value="modelValue"
    :title="editing ? '修改评价' : '评价商品'"
    width="560px"
    @update:model-value="$emit('update:modelValue', $event)"
    @cancel="$emit('update:modelValue', false)"
  >
    <div class="review-form">
      <div class="product-copy">
        <strong>{{ eligibility?.productTitle || '已购商品' }}</strong>
        <span v-if="eligibility?.skuTitle">{{ eligibility.skuTitle }}</span>
      </div>
      <label>
        <span>商品评分</span>
        <StarRating v-model="rating" />
        <small v-if="ratingError" class="field-error">{{ ratingError }}</small>
      </label>
      <label>
        <span>评价内容</span>
        <PixelTextarea
          v-model="content"
          :rows="6"
          :maxlength="1000"
          placeholder="说说商品的实际使用感受"
          :error="contentError"
        />
        <small class="counter">{{ content.length }} / 1000</small>
      </label>
    </div>
    <template #footer>
      <PixelButton type="ghost" @click="$emit('update:modelValue', false)">取消</PixelButton>
      <PixelButton data-test="submit-review" :loading="saving" @click="submit">
        {{ editing ? '保存修改' : '提交评价' }}
      </PixelButton>
    </template>
  </PixelDialog>
</template>

<style scoped>
.review-form{display:grid;gap:var(--sp-4)}.product-copy{display:grid;gap:4px;padding-bottom:var(--sp-3);border-bottom:2px solid var(--c-line)}.product-copy strong{font-family:var(--font-pixel);font-size:18px;color:var(--c-wood-dark)}.product-copy span,.counter{color:var(--c-ink-2);font-size:12px}.review-form label{display:grid;gap:8px}.review-form label>span{font-weight:var(--fw-bold);color:var(--c-ink)}.counter{text-align:right}.field-error{color:var(--c-brick);font-size:12px}
</style>
