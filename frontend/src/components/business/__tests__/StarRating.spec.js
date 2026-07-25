import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StarRating from '../StarRating.vue'

describe('StarRating', () => {
  it('announces average ratings and the unrated state', () => {
    const unrated = mount(StarRating, { props: { modelValue: 0, readonly: true } })
    expect(unrated.get('[role="img"]').attributes('aria-label')).toBe('暂无评分')

    const rated = mount(StarRating, { props: { modelValue: 4.6, readonly: true } })
    expect(rated.get('[role="img"]').attributes('aria-label')).toContain('4.6')
  })

  it('renders five labelled buttons in editable mode', async () => {
    const wrapper = mount(StarRating, { props: { modelValue: 2 } })
    const buttons = wrapper.findAll('button')
    expect(buttons).toHaveLength(5)
    expect(buttons.map((button) => button.attributes('aria-label')))
      .toEqual(['评 1 星', '评 2 星', '评 3 星', '评 4 星', '评 5 星'])

    await buttons[3].trigger('click')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([4])
  })

  it('supports arrow, Home and End keyboard changes', async () => {
    const wrapper = mount(StarRating, { props: { modelValue: 3 } })
    const current = wrapper.findAll('button')[2]

    await current.trigger('keydown', { key: 'ArrowRight' })
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([4])
    await current.trigger('keydown', { key: 'ArrowLeft' })
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([2])
    await current.trigger('keydown', { key: 'Home' })
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([1])
    await current.trigger('keydown', { key: 'End' })
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual([5])
  })
})
