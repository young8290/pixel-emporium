<script setup>
defineProps({ page: { type: Number, default: 1 }, size: { type: Number, default: 12 }, total: { type: Number, default: 0 } })
defineEmits(['update:page', 'change'])
</script>
<template>
  <el-pagination class="pixel-pagination" background layout="prev, pager, next"
    :current-page="page" :page-size="size" :total="total"
    @current-change="$emit('update:page', $event);$emit('change', $event)" />
</template>
<style scoped>
.pixel-pagination{justify-content:flex-end;margin-top:var(--sp-5)}.pixel-pagination :deep(.btn-prev),.pixel-pagination :deep(.btn-next),.pixel-pagination :deep(.el-pager li){border:2px solid var(--c-wood);border-radius:var(--radius);background:var(--c-surface);color:var(--c-ink)}.pixel-pagination :deep(.el-pager li.is-active){background:var(--c-moss);color:var(--c-surface)}
</style>
