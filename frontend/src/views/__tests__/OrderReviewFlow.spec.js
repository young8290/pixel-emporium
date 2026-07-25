import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OrderDetailView from '../user/OrderDetailView.vue'

const orderApi = vi.hoisted(() => ({
  getOrder: vi.fn(), cancelOrder: vi.fn(), confirmOrder: vi.fn()
}))
const reviewApi = vi.hoisted(() => ({
  getOrderReviewEligibility: vi.fn(), deleteReview: vi.fn()
}))
const route = vi.hoisted(() => ({ params: { id: '501' } }))
const router = vi.hoisted(() => ({ back: vi.fn(), push: vi.fn(), replace: vi.fn() }))

vi.mock('@/api/order', () => orderApi)
vi.mock('@/api/review', () => reviewApi)
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => router }))

const order = (status = 'COMPLETED') => ({
  id: '501', orderNo: 'NO501', status, totalAmount: 30, payAmount: 30, freightAmount: 0,
  items: [
    { id: '701', productId: '301', productTitle: '像素键盘', skuTitle: '森林绿', price: 10, quantity: 1 },
    { id: '702', productId: '302', productTitle: '像素鼠标', skuTitle: '米白', price: 10, quantity: 1 },
    { id: '703', productId: '303', productTitle: '像素耳机', skuTitle: '黑色', price: 10, quantity: 1 }
  ]
})

const eligibility = {
  orderId: '501',
  items: [
    { orderItemId: '701', productId: '301', eligible: true, editable: false, deletable: false },
    { orderItemId: '702', productId: '302', eligible: false, editable: true, deletable: true, reviewId: '88', reviewStatus: 'PUBLISHED', rating: 5, content: '很好用的商品' },
    { orderItemId: '703', productId: '303', eligible: false, editable: false, deletable: true, reviewId: '89', reviewStatus: 'HIDDEN', moderationReason: '包含不当内容' }
  ]
}

const buttonStub = {
  props: ['loading', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="loading || disabled" @click="$emit(\'click\')"><slot/></button>'
}
const confirmStub = {
  props: ['modelValue'],
  emits: ['confirm', 'cancel'],
  template: '<button v-if="modelValue" data-test="confirm-delete" @click="$emit(\'confirm\')">确认</button>'
}
const reviewDialogStub = {
  props: ['modelValue', 'eligibility'],
  emits: ['saved', 'update:modelValue'],
  template: '<button v-if="modelValue" data-test="dialog-saved" @click="$emit(\'saved\')">保存</button>'
}
const stubs = {
  PixelButton: buttonStub,
  PixelConfirmDialog: confirmStub,
  ProductReviewDialog: reviewDialogStub,
  PixelSkeleton: { template: '<div />' },
  PixelErrorState: { template: '<div />' },
  OrderTimeline: { template: '<div />' }
}

describe('Order detail review flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    orderApi.getOrder.mockResolvedValue(order())
    reviewApi.getOrderReviewEligibility.mockResolvedValue(eligibility)
    reviewApi.deleteReview.mockResolvedValue(true)
  })

  it('loads eligibility only for completed orders and renders state-specific actions', async () => {
    const wrapper = mount(OrderDetailView, { global: { stubs } })
    await flushPromises()

    expect(reviewApi.getOrderReviewEligibility).toHaveBeenCalledWith('501')
    expect(wrapper.text()).toContain('评价商品')
    expect(wrapper.text()).toContain('修改评价')
    expect(wrapper.text()).toContain('删除评价')
    expect(wrapper.text()).toContain('包含不当内容')
    const hiddenRow = wrapper.findAll('.order-items article')[2]
    expect(hiddenRow.text()).not.toContain('修改评价')

    orderApi.getOrder.mockResolvedValueOnce(order('SHIPPED'))
    reviewApi.getOrderReviewEligibility.mockClear()
    mount(OrderDetailView, { global: { stubs } })
    await flushPromises()
    expect(reviewApi.getOrderReviewEligibility).not.toHaveBeenCalled()
  })

  it('confirms deletion and refreshes eligibility after mutations', async () => {
    const wrapper = mount(OrderDetailView, { global: { stubs } })
    await flushPromises()
    const deleteButtons = wrapper.findAll('button').filter((button) => button.text() === '删除评价')
    await deleteButtons[0].trigger('click')
    await wrapper.get('[data-test="confirm-delete"]').trigger('click')
    await flushPromises()

    expect(reviewApi.deleteReview).toHaveBeenCalledWith('88')
    expect(reviewApi.getOrderReviewEligibility).toHaveBeenCalledTimes(2)

    const createButton = wrapper.findAll('button').find((button) => button.text() === '评价商品')
    await createButton.trigger('click')
    await wrapper.get('[data-test="dialog-saved"]').trigger('click')
    await flushPromises()
    expect(reviewApi.getOrderReviewEligibility).toHaveBeenCalledTimes(3)
  })
})
