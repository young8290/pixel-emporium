import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import QtyStepper from '../QtyStepper.vue'

describe('QtyStepper', () => {
  it('respects min and max boundaries', async () => {
    const wrapper = mount(QtyStepper, { props: { modelValue: 1, min: 1, max: 2 } })
    expect(wrapper.findAll('button')[0].attributes('disabled')).toBeDefined()
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([2])
    await wrapper.setProps({ modelValue: 2 })
    expect(wrapper.findAll('button')[1].attributes('disabled')).toBeDefined()
  })

  it('clamps direct input', async () => {
    const wrapper = mount(QtyStepper, { props: { modelValue: 2, min: 1, max: 5 } })
    await wrapper.get('input').setValue(20)
    await wrapper.get('input').trigger('change')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([5])
  })
})
