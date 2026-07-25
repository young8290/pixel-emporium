import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatCard from '../StatCard.vue'

describe('StatCard', () => {
  it('renders label, value and hint', () => {
    const wrapper = mount(StatCard, { props: { label: '今日订单', value: 12, hint: '累计 99' } })
    expect(wrapper.text()).toContain('今日订单')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('累计 99')
  })
})
