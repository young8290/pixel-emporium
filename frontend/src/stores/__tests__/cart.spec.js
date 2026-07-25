import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/cart', () => ({
  getCart: vi.fn(),
  addCartItem: vi.fn(),
  updateCartItem: vi.fn(),
  removeCartItem: vi.fn(),
  removeCartItems: vi.fn(),
  clearCart: vi.fn()
}))

import * as api from '@/api/cart'
import { useCartStore } from '../cart'

describe('cart store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('hydrates totals and derives count/checked items', async () => {
    api.getCart.mockResolvedValue({
      items: [{ id: 1, quantity: 2, checked: true }, { id: 2, quantity: 3, checked: false }],
      checkedAmount: 19.8,
      checkedQuantity: 2
    })
    const store = useCartStore()
    await store.fetch()
    expect(store.count).toBe(5)
    expect(store.checkedItems.map((item) => item.id)).toEqual([1])
    expect(store.checkedAmount).toBe(19.8)
  })

  it('adds then refreshes the authoritative cart', async () => {
    api.getCart.mockResolvedValue({ items: [], checkedAmount: 0, checkedQuantity: 0 })
    const store = useCartStore()
    await store.add(9, 2)
    expect(api.addCartItem).toHaveBeenCalledWith(9, 2)
    expect(api.getCart).toHaveBeenCalledOnce()
  })
})
