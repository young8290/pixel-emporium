import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SimpleChart from '../SimpleChart.vue'

describe('SimpleChart', () => {
  it('creates one marker per value', () => {
    const wrapper = mount(SimpleChart, { props: { values: [1, 2, 3], labels: ['一', '二', '三'] } })
    expect(wrapper.findAll('circle')).toHaveLength(3)
    expect(wrapper.find('polyline').attributes('points')).toBeTruthy()
  })
})
