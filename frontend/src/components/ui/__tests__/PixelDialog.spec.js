import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PixelDialog from '../PixelDialog.vue'

const dialogStub = {
  name: 'ElDialog',
  props: ['modelValue', 'title', 'width'],
  emits: ['update:modelValue', 'close'],
  template: '<section><slot/><slot name="footer"/></section>'
}

describe('PixelDialog', () => {
  it('constrains configured pixel widths to the mobile viewport', () => {
    const wrapper = mount(PixelDialog, {
      props: { modelValue: true, width: '560px' },
      global: { stubs: { 'el-dialog': dialogStub } }
    })

    const dialog = wrapper.findComponent(dialogStub)
    expect(dialog.props('width')).toBe('min(560px, calc(100vw - 24px))')
  })
})
