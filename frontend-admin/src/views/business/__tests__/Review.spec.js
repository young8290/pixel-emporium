import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Review from '../Review.vue'

const api = vi.hoisted(() => ({
  listReviews: vi.fn(),
  hideReview: vi.fn(),
  restoreReview: vi.fn()
}))
const messageBox = vi.hoisted(() => ({ confirm: vi.fn() }))

vi.mock('@/api/business/review', () => api)
vi.mock('element-plus', async importOriginal => ({
  ...await importOriginal(),
  ElMessageBox: messageBox
}))

describe('Review moderation page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listReviews.mockResolvedValue({
      list: [reviewRow()],
      total: 21
    })
    api.hideReview.mockResolvedValue(true)
    api.restoreReview.mockResolvedValue(true)
    messageBox.confirm.mockResolvedValue('confirm')
  })

  it('sends all filters, resets filtered results to page one and loads changed pages', async () => {
    const wrapper = mountReview()
    await flushPromises()

    Object.assign(wrapper.vm.query, {
      page: 4,
      productId: '12',
      userId: '34',
      rating: 2,
      status: 'HIDDEN',
      keyword: '包装',
      createdAtStart: '2026-07-01T00:00:00',
      createdAtEnd: '2026-07-24T23:59:59'
    })
    await wrapper.vm.applyFilters()

    expect(api.listReviews).toHaveBeenLastCalledWith({
      page: 1,
      size: 10,
      productId: '12',
      userId: '34',
      rating: 2,
      status: 'HIDDEN',
      keyword: '包装',
      createdAtStart: '2026-07-01T00:00:00',
      createdAtEnd: '2026-07-24T23:59:59'
    })

    await wrapper.vm.changePage(3)
    expect(api.listReviews).toHaveBeenLastCalledWith(expect.objectContaining({ page: 3 }))
    expect(wrapper.vm.total).toBe(21)
  })

  it('opens a detail drawer and renders review content as text', async () => {
    const wrapper = mountReview()
    await flushPromises()
    const row = reviewRow({ content: '<img src=x onerror=alert(1)>' })

    wrapper.vm.openDetails(row)
    await wrapper.vm.$nextTick()

    const content = wrapper.get('[data-testid="review-content"]')
    expect(content.text()).toBe(row.content)
    expect(content.find('img').exists()).toBe(false)
  })

  it('requires a valid reason before hiding and refreshes after moderation', async () => {
    const wrapper = mountReview()
    await flushPromises()
    wrapper.vm.openHide(reviewRow())

    wrapper.vm.hideReason = ' '
    await wrapper.vm.submitHide()
    expect(api.hideReview).not.toHaveBeenCalled()
    expect(wrapper.vm.reasonError).toContain('2')

    wrapper.vm.hideReason = '  广告  '
    await wrapper.vm.submitHide()

    expect(messageBox.confirm).toHaveBeenCalled()
    expect(api.hideReview).toHaveBeenCalledWith('88', '广告')
    expect(api.listReviews).toHaveBeenCalledTimes(2)
    expect(wrapper.vm.hideOpen).toBe(false)
  })

  it('confirms restoration, refreshes state and always releases loading', async () => {
    const wrapper = mountReview()
    await flushPromises()

    await wrapper.vm.restore(reviewRow({ id: '99' }))
    expect(messageBox.confirm).toHaveBeenCalled()
    expect(api.restoreReview).toHaveBeenCalledWith('99')
    expect(api.listReviews).toHaveBeenCalledTimes(2)

    api.listReviews.mockRejectedValueOnce(new Error('network'))
    await expect(wrapper.vm.load()).rejects.toThrow('network')
    expect(wrapper.vm.loading).toBe(false)
  })

  it('keeps moderation overlays inside a narrow viewport', async () => {
    const wrapper = mountReview()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'ElDrawer' }).props('size'))
      .toBe('min(520px, 100vw)')
    expect(wrapper.findComponent({ name: 'ElDialog' }).props('width'))
      .toBe('min(520px, calc(100vw - 24px))')
  })
})

function reviewRow(overrides = {}) {
  return {
    id: '88',
    productId: '12',
    productTitle: '像素牧场杯',
    skuTitle: '绿色',
    userId: '34',
    orderId: '56',
    orderItemId: '78',
    rating: 4,
    content: '包装完整，杯子很好看。',
    status: 'PUBLISHED',
    moderationReason: '',
    createdAt: '2026-07-24 10:00:00',
    ...overrides
  }
}

function mountReview() {
  return mount(Review, {
    global: {
      stubs: {
        FilterBar: { template: '<div><slot/><slot name="actions"/></div>' },
        'el-input': true,
        'el-select': { template: '<select><slot/></select>' },
        'el-option': true,
        'el-date-picker': true,
        'el-button': { template: '<button @click="$emit(\'click\')"><slot/></button>' },
        'el-table': { template: '<div><slot/></div>' },
        'el-table-column': true,
        'el-pagination': true,
        'el-tag': true,
        'el-dialog': { name: 'ElDialog', props: ['modelValue', 'width'], template: '<div v-if="modelValue"><slot/><slot name="footer"/></div>' },
        'el-drawer': { name: 'ElDrawer', props: ['modelValue', 'size'], template: '<aside v-if="modelValue"><slot/></aside>' },
        'el-descriptions': { template: '<dl><slot/></dl>' },
        'el-descriptions-item': { template: '<div><slot/></div>' }
      }
    }
  })
}
