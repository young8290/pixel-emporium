<script setup>
import { ref, onMounted } from 'vue'
import { hi } from '@/api/hello.js'

const fromHi = ref('')

const getHi = async () => {
  try {
    const res = await hi();
    fromHi.value = res.toString()
  }catch (error){
    console.error('请求 hi 接口失败', error)
  }
}

onMounted(() => {
  getHi()
})
</script>

<template>
  <div>
    <h1>仪表盘</h1>
    <p>来自 hi 接口的响应：{{ fromHi }}</p>
  </div>
</template>

<style scoped>

</style>
