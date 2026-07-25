import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PixelButton from '../PixelButton.vue'

describe('PixelButton', () => {
  it('emits click when available', async () => {
    const wrapper = mount(PixelButton, { slots: { default: '收下' } })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('emits one click when Enter is pressed', async () => {
    const wrapper = mount(PixelButton, { slots: { default: '收下' } })
    await wrapper.get('button').trigger('keydown', { key: 'Enter' })
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it.each([{ disabled: true }, { loading: true }])('blocks click for %o', async (props) => {
    const wrapper = mount(PixelButton, { props })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })
})
