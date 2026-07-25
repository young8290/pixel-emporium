import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import OrderCard from '../OrderCard.vue'

describe('OrderCard status actions', () => {
  it('shows pay/cancel only for pending payment', () => {
    const wrapper = mount(OrderCard, { props: { order: { id: 1, status: 'PENDING_PAYMENT', items: [] } } })
    expect(wrapper.text()).toContain('付款')
    expect(wrapper.text()).toContain('取消')
    expect(wrapper.text()).not.toContain('确认收货')
  })

  it('shows receive confirmation only after shipment', () => {
    const wrapper = mount(OrderCard, { props: { order: { id: 1, status: 'SHIPPED', items: [] } } })
    expect(wrapper.text()).toContain('确认收货')
    expect(wrapper.text()).not.toContain('付款')
  })
})
