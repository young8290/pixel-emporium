import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ProductRecommendations from '../ProductRecommendations.vue'

const api = vi.hoisted(() => ({ getProductRecommendations: vi.fn() }))
vi.mock('@/api/product', () => api)

describe('ProductRecommendations', () => {
  it('renders recommendation products returned for the current product', async () => {
    api.getProductRecommendations.mockResolvedValue([
      { id: '8', title: '像素台灯', price: 88 },
      { id: '9', title: '像素花盆', price: 28 },
    ])
    const wrapper = mount(ProductRecommendations, {
      props: { productId: '7' },
      global: {
        stubs: {
          ProductCard: { props: ['product'], template: '<article>{{ product.title }}</article>' },
        },
      },
    })
    await flushPromises()

    expect(api.getProductRecommendations).toHaveBeenCalledWith('7', 8)
    expect(wrapper.text()).toContain('像素台灯')
    expect(wrapper.text()).toContain('像素花盆')
  })
})
