<script setup>
import { computed, nextTick, ref } from 'vue'
import starIcon from '@/assets/pixel/icons/icon-star.png'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'change'])
const buttons = ref([])
const normalized = computed(() => Math.min(5, Math.max(0, Number(props.modelValue) || 0)))
const current = computed(() => Math.min(5, Math.max(1, Math.round(normalized.value) || 1)))
const label = computed(() => normalized.value > 0
  ? `评分 ${normalized.value} 分（满分 5 分）`
  : '暂无评分')

function select(value) {
  const next = Math.min(5, Math.max(1, value))
  emit('update:modelValue', next)
  emit('change', next)
  nextTick(() => buttons.value[next - 1]?.focus())
}

function onKeydown(event) {
  const next = {
    ArrowRight: current.value + 1,
    ArrowUp: current.value + 1,
    ArrowLeft: current.value - 1,
    ArrowDown: current.value - 1,
    Home: 1,
    End: 5
  }[event.key]
  if (next == null) return
  event.preventDefault()
  select(next)
}
</script>

<template>
  <div v-if="readonly" class="star-rating is-readonly" role="img" :aria-label="label">
    <img
      v-for="star in 5"
      :key="star"
      :src="starIcon"
      alt=""
      aria-hidden="true"
      data-pixel
      :class="{ 'is-empty': star > Math.round(normalized) }"
    >
  </div>
  <div v-else class="star-rating is-editable" role="radiogroup" aria-label="商品评分">
    <button
      v-for="star in 5"
      :key="star"
      :ref="(element) => { if (element) buttons[star - 1] = element }"
      type="button"
      role="radio"
      :aria-label="`评 ${star} 星`"
      :aria-checked="star === current"
      :tabindex="star === current ? 0 : -1"
      @click="select(star)"
      @keydown="onKeydown"
    >
      <img
        :src="starIcon"
        alt=""
        aria-hidden="true"
        data-pixel
        :class="{ 'is-empty': star > normalized }"
      >
    </button>
  </div>
</template>

<style scoped>
.star-rating{display:inline-flex;align-items:center;gap:4px;min-height:28px}.star-rating img{display:block;width:24px;height:24px;object-fit:contain;transition:opacity var(--dur-1) var(--ease-std)}.star-rating img.is-empty{opacity:.25}.star-rating button{display:grid;place-items:center;width:32px;height:32px;padding:0;border:0;background:transparent;cursor:pointer}.star-rating button:hover img{opacity:.75}.star-rating button:focus-visible{outline:2px solid var(--c-amber);outline-offset:0}@media(prefers-reduced-motion:reduce){.star-rating img{transition:none}}
</style>
