import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as cartApi from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const checkedAmount = ref(0)
  const checkedQuantity = ref(0)
  const loading = ref(false)
  const count = computed(() => items.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
  const checkedItems = computed(() => items.value.filter((item) => item.checked && !item.invalid))

  async function fetch() {
    loading.value = true
    try {
      const data = await cartApi.getCart()
      items.value = data?.items || []
      checkedAmount.value = Number(data?.checkedAmount || 0)
      checkedQuantity.value = Number(data?.checkedQuantity || 0)
      return data
    } finally {
      loading.value = false
    }
  }

  async function add(productSkuId, quantity = 1) {
    await cartApi.addCartItem(productSkuId, quantity)
    return fetch()
  }

  async function update(payload) {
    await cartApi.updateCartItem(payload)
    return fetch()
  }

  async function remove(id) {
    await cartApi.removeCartItem(id)
    return fetch()
  }

  async function removeBatch(ids) {
    await cartApi.removeCartItems(ids)
    return fetch()
  }

  async function clear() {
    await cartApi.clearCart()
    return fetch()
  }

  function reset() {
    items.value = []
    checkedAmount.value = 0
    checkedQuantity.value = 0
  }

  return {
    items, checkedAmount, checkedQuantity, checkedItems, count, loading,
    fetch, add, update, remove, removeBatch, clear, reset
  }
})
