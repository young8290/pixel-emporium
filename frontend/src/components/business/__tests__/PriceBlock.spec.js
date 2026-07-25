import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PriceBlock from '../PriceBlock.vue'

describe('PriceBlock discount display', () => {
  it('shows the crossed original price and calculated discount', () => {
    const wrapper = mount(PriceBlock, { props: { price: 80, original: 100 } })

    expect(wrapper.find('del').text()).toBe('¥100.00')
    expect(wrapper.find('[data-test="discount-badge"]').text()).toBe('8折')
  })

  it('hides discount decoration when original price is not higher', () => {
    const wrapper = mount(PriceBlock, { props: { price: 100, original: 100 } })

    expect(wrapper.find('del').exists()).toBe(false)
    expect(wrapper.find('[data-test="discount-badge"]').exists()).toBe(false)
  })
})
