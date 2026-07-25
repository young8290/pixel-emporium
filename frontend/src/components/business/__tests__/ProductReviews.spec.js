import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductReviewList from '../ProductReviewList.vue'
import ProductReviewSummary from '../ProductReviewSummary.vue'
import ProductReviews from '../ProductReviews.vue'

const api = vi.hoisted(() => ({
  getReviewSummary: vi.fn(),
  listProductReviews: vi.fn()
}))

vi.mock('@/api/review', () => api)

const paginationStub = {
  name: 'PixelPagination',
  props: ['page', 'size', 'total'],
  emits: ['change'],
  template: '<button class="next-page" @click="$emit(\'change\', 2)">next</button>'
}

describe('Product review components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getReviewSummary.mockResolvedValue({
      averageRating: 0,
      reviewCount: 0,
      distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
    })
    api.listProductReviews.mockResolvedValue({ total: 0, list: [] })
  })

  it('keeps empty distribution widths finite', () => {
    const wrapper = mount(ProductReviewSummary, {
      props: { summary: { averageRating: 0, reviewCount: 0, distribution: {} } }
    })
    const widths = wrapper.findAll('.distribution-fill').map((bar) => bar.attributes('style'))
    expect(widths.every((style) => style.includes('width: 0%'))).toBe(true)
    expect(wrapper.text()).toContain('暂无评分')
  })

  it('renders loading, error, empty and escaped review content states', async () => {
    const loading = mount(ProductReviewList, { props: { loading: true } })
    expect(loading.find('.reviews-loading').exists()).toBe(true)
    const error = mount(ProductReviewList, { props: { error: true } })
    expect(error.find('.reviews-error').exists()).toBe(true)
    const empty = mount(ProductReviewList, { props: { reviews: [] } })
    expect(empty.find('.reviews-empty').exists()).toBe(true)

    const content = '<img src=x onerror=alert(1)>'
    const list = mount(ProductReviewList, {
      props: { reviews: [{ id: '1', rating: 5, content, reviewerLabel: '已购用户' }] }
    })
    expect(list.text()).toContain(content)
    expect(list.find('.review-content img').exists()).toBe(false)
  })

  it('reloads reviews and resets page when sort or rating changes', async () => {
    api.listProductReviews.mockResolvedValue({ total: 25, list: [] })
    const wrapper = mount(ProductReviews, {
      props: { productId: '301' },
      global: { stubs: { PixelPagination: paginationStub } }
    })
    await flushPromises()
    await wrapper.get('.next-page').trigger('click')
    await flushPromises()
    expect(api.listProductReviews.mock.calls.at(-1)[1].page).toBe(2)

    await wrapper.get('[data-test="review-sort"]').setValue('highest')
    await flushPromises()
    expect(api.listProductReviews.mock.calls.at(-1)[1]).toMatchObject({
      page: 1,
      sort: 'highest'
    })

    await wrapper.get('[data-test="review-rating"]').setValue('5')
    await flushPromises()
    expect(api.listProductReviews.mock.calls.at(-1)[1]).toMatchObject({ page: 1, rating: 5 })
  })

  it('shows request failures instead of an empty list', async () => {
    api.listProductReviews.mockRejectedValueOnce(new Error('network'))
    const wrapper = mount(ProductReviews, {
      props: { productId: '301' },
      global: { stubs: { PixelPagination: paginationStub } }
    })
    await flushPromises()
    expect(wrapper.find('.reviews-error').exists()).toBe(true)
  })
})
