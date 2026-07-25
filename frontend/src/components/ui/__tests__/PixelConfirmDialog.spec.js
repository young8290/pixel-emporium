import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PixelConfirmDialog from '../PixelConfirmDialog.vue'

const PixelDialogStub = {
  template: '<section><slot/><slot name="footer"/></section>'
}

describe('PixelConfirmDialog', () => {
  it('emits cancel and confirm actions', async () => {
    const wrapper = mount(PixelConfirmDialog, {
      props: { modelValue: true, content: '继续吗？' },
      global: { stubs: { PixelDialog: PixelDialogStub } }
    })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
    expect(wrapper.emitted('confirm')).toHaveLength(1)
  })
})
