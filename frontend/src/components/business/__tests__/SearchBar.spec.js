import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import SearchBar from '../SearchBar.vue'

const api = vi.hoisted(() => ({ getProductSuggestions: vi.fn() }))
vi.mock('@/api/product', () => api)

describe('SearchBar suggestions', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('loads suggestions after debounce and searches the keyboard selection', async () => {
    vi.useFakeTimers()
    api.getProductSuggestions.mockResolvedValue(['月光台灯', '灯具'])
    const wrapper = mount(SearchBar)

    await wrapper.get('input').setValue('灯')
    await vi.advanceTimersByTimeAsync(280)
    await flushPromises()

    expect(api.getProductSuggestions).toHaveBeenCalledWith('灯', 10)
    expect(wrapper.findAll('[data-test="search-suggestion"]')).toHaveLength(2)
    await wrapper.get('input').trigger('keydown', { key: 'ArrowDown' })
    await wrapper.get('input').trigger('keydown', { key: 'Enter' })

    expect(wrapper.emitted('search')?.at(-1)).toEqual(['月光台灯'])
  })
})
