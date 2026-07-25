<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { getProductSuggestions } from '@/api/product'
import searchIcon from '@/assets/pixel/icons/icon-search.png'
const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue', 'search'])
const keyword = ref(props.modelValue)
const suggestions = ref([])
const open = ref(false)
const activeIndex = ref(-1)
let debounceTimer
let closeTimer
let requestSequence = 0

watch(() => props.modelValue, (value) => { keyword.value = value })

watch(keyword, (value) => {
  clearTimeout(debounceTimer)
  const normalized = value.trim()
  const sequence = ++requestSequence
  activeIndex.value = -1
  if (!normalized) {
    suggestions.value = []
    open.value = false
    return
  }
  debounceTimer = setTimeout(async () => {
    try {
      const result = await getProductSuggestions(normalized, 10)
      if (sequence !== requestSequence || keyword.value.trim() !== normalized) return
      suggestions.value = Array.isArray(result) ? result : []
      open.value = suggestions.value.length > 0
    } catch {
      if (sequence === requestSequence) {
        suggestions.value = []
        open.value = false
      }
    }
  }, 280)
})

function submit() {
  const value = keyword.value.trim()
  emit('update:modelValue', value)
  if (value) {
    open.value = false
    emit('search', value)
  }
}

function choose(value) {
  keyword.value = value
  suggestions.value = []
  open.value = false
  emit('update:modelValue', value)
  emit('search', value)
}

function handleKeydown(event) {
  if (!open.value || !suggestions.value.length) return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % suggestions.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + suggestions.value.length) % suggestions.value.length
  } else if (event.key === 'Enter' && activeIndex.value >= 0) {
    event.preventDefault()
    choose(suggestions.value[activeIndex.value])
  } else if (event.key === 'Escape') {
    open.value = false
  }
}

function scheduleClose() {
  clearTimeout(closeTimer)
  closeTimer = setTimeout(() => { open.value = false }, 120)
}

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearTimeout(closeTimer)
})
</script>
<template>
  <form class="search-bar" role="search" @submit.prevent="submit">
    <input v-model="keyword" type="search" placeholder="找找今天的好物…" aria-label="搜索商品" autocomplete="off" aria-autocomplete="list" :aria-expanded="open" @focus="open = suggestions.length > 0" @blur="scheduleClose" @keydown="handleKeydown">
    <button type="submit" aria-label="搜索"><img :src="searchIcon" alt="" data-pixel></button>
    <ul v-if="open" class="suggestions" role="listbox">
      <li v-for="(suggestion,index) in suggestions" :key="suggestion">
        <button type="button" role="option" data-test="search-suggestion" :aria-selected="index === activeIndex" :class="{ active: index === activeIndex }" @mousedown.prevent="choose(suggestion)">{{ suggestion }}</button>
      </li>
    </ul>
  </form>
</template>
<style scoped>
.search-bar{position:relative;display:flex;width:min(440px,100%);height:42px}.search-bar>input{min-width:0;flex:1;padding:0 14px;border:2px solid var(--c-wood-dark);border-right:0;border-radius:var(--radius) 0 0 var(--radius);background:var(--c-surface);color:var(--c-ink);font:inherit;outline:0}.search-bar>input:focus{box-shadow:inset 0 0 0 2px var(--c-amber)}.search-bar>button{width:48px;border:2px solid var(--c-wood-dark);border-radius:0 var(--radius) var(--radius) 0;background:var(--c-moss);cursor:pointer}.search-bar img{width:24px;height:24px}.suggestions{position:absolute;z-index:50;top:40px;left:0;right:48px;overflow:hidden;margin:0;padding:4px;border:2px solid var(--c-wood-dark);background:var(--c-surface);box-shadow:var(--shadow-pixel);list-style:none}.suggestions button{width:100%;min-height:38px;padding:6px 10px;border:0;background:transparent;color:var(--c-ink);font:inherit;text-align:left;cursor:pointer}.suggestions button:hover,.suggestions button.active{background:var(--c-moss);color:var(--c-surface)}
</style>
