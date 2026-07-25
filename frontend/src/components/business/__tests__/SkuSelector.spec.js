import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import SkuSelector from '../SkuSelector.vue'

const attributes = [
  { id: 1, name: '口味', values: [{ id: 11, value: '甜' }, { id: 12, value: '咸' }] },
  { id: 2, name: '份量', values: [{ id: 21, value: '小' }, { id: 22, value: '大' }] }
]
const skus = [
  { id: 101, stock: 3, attributeValueIds: [11, 21] },
  { id: 102, stock: 0, attributeValueIds: [12, 22] }
]

describe('SkuSelector', () => {
  it('selects a complete in-stock combination', async () => {
    const wrapper = mount(SkuSelector, { props: { attributes, skus } })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[2].trigger('click')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([skus[0]])
  })

  it('disables values without any in-stock combination', () => {
    const wrapper = mount(SkuSelector, { props: { attributes, skus } })
    expect(wrapper.findAll('button')[1].attributes('disabled')).toBeDefined()
  })
})
