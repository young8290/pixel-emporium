import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FilterBar from '../FilterBar.vue'

describe('FilterBar', () => {
  it('offers rating sort and emits price and stock filters', async () => {
    const wrapper = mount(FilterBar)

    expect(wrapper.find('[data-sort="rating"]').exists()).toBe(true)
    await wrapper.get('[data-test="min-price"]').setValue('10')
    await wrapper.get('[data-test="max-price"]').setValue('80')
    await wrapper.get('[data-test="in-stock"]').setValue(true)
    await wrapper.get('[data-test="apply-filters"]').trigger('click')

    expect(wrapper.emitted('apply')?.at(-1)).toEqual([{
      minPrice: '10',
      maxPrice: '80',
      inStock: true,
    }])
  })
})
