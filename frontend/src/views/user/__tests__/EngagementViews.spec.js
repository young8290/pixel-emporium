import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FavoritesView from '../FavoritesView.vue'
import BrowseHistoryView from '../BrowseHistoryView.vue'

const api = vi.hoisted(() => ({
  listFavorites: vi.fn(),
  removeFavorite: vi.fn(),
  listBrowseHistory: vi.fn(),
  removeBrowseHistory: vi.fn(),
  clearBrowseHistory: vi.fn(),
}))

vi.mock('@/api/engagement', () => api)

const stubs = {
  ProductCard: { props: ['product'], template: '<article data-test="product-card">{{ product.title }}</article>' },
  PixelSkeleton: { template: '<div />' },
  PixelErrorState: { template: '<div />' },
  PixelEmptyState: { template: '<div />' },
  PixelPagination: { template: '<div />' },
  PixelButton: { template: '<button><slot /></button>' },
  PixelConfirmDialog: {
    props: ['modelValue'],
    emits: ['confirm'],
    template: '<button v-if="modelValue" data-test="confirm-clear" @click="$emit(\'confirm\')">确认</button>'
  },
}

describe('favorite and browse history views', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listFavorites.mockResolvedValue({
      list: [{ id: '7', title: '像素台灯', price: 80, onSale: true }],
      total: 1,
    })
    api.listBrowseHistory.mockResolvedValue({
      list: [{ id: '8', title: '像素花盆', price: 20, onSale: true, viewCount: 3, lastViewedAt: '2026-07-24 20:00:00' }],
      total: 1,
    })
    api.removeFavorite.mockResolvedValue(true)
    api.removeBrowseHistory.mockResolvedValue(true)
    api.clearBrowseHistory.mockResolvedValue(true)
  })

  it('lists favorites and removes one product', async () => {
    const wrapper = mount(FavoritesView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('像素台灯')
    await wrapper.get('[data-test="remove-favorite"]').trigger('click')
    await flushPromises()

    expect(api.removeFavorite).toHaveBeenCalledWith('7')
    expect(api.listFavorites).toHaveBeenCalledTimes(2)
  })

  it('lists history and clears it after confirmation', async () => {
    const wrapper = mount(BrowseHistoryView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('浏览 3 次')
    await wrapper.get('[data-test="open-clear-history"]').trigger('click')
    await wrapper.get('[data-test="confirm-clear"]').trigger('click')
    await flushPromises()

    expect(api.clearBrowseHistory).toHaveBeenCalledOnce()
    expect(api.listBrowseHistory).toHaveBeenCalledTimes(2)
  })
})
