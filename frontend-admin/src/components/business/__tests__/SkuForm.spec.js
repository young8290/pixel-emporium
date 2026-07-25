import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SkuForm from '../SkuForm.vue'

describe('SkuForm discount price', () => {
  it('provides an optional two-decimal original price input', () => {
    const form = {
      id: null,
      productId: '7',
      title: '默认规格',
      skuCode: '',
      price: 8.5,
      originalPrice: 12,
      stock: 10,
    }
    const wrapper = mount(SkuForm, {
      props: { form, products: [] },
      global: {
        stubs: {
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<label><slot /></label>' },
          'el-select': true,
          'el-option': true,
          'el-input': true,
          'el-input-number': true,
        },
      },
    })

    const originalPrice = wrapper.find('[data-test="original-price"]')
    expect(originalPrice.exists()).toBe(true)
    expect(originalPrice.attributes('modelvalue')).toBe('12')
    expect(originalPrice.attributes('precision')).toBe('2')
  })
})
