import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import ThemeSwitcher from '../ThemeSwitcher.vue'
import { useThemeStore } from '@/stores/theme'

describe('ThemeSwitcher', () => {
  let pinia

  beforeEach(() => {
    localStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it('keeps seasonal and day/night controls in sync with the store', async () => {
    const store = useThemeStore()
    store.setMode('day')
    const wrapper = mount(ThemeSwitcher, { global: { plugins: [pinia] } })

    await wrapper.get('[aria-label="切换到冬季"]').trigger('click')
    expect(store.season).toBe('winter')
    expect(wrapper.get('[aria-label="切换到冬季"]').attributes('aria-pressed')).toBe('true')

    await wrapper.get('[aria-label="切换到夜间"]').trigger('click')
    expect(store.mode).toBe('night')
    expect(wrapper.get('[aria-label="切换到日间"]').attributes('aria-pressed')).toBe('true')
  })

  it('returns both controls to automatic mode', async () => {
    const store = useThemeStore()
    store.setSeason('spring')
    store.setMode('night')
    const wrapper = mount(ThemeSwitcher, { global: { plugins: [pinia] } })

    await wrapper.get('[aria-label="使用自动主题"]').trigger('click')
    expect(store.season).toBe('')
    expect(store.mode).toBe('')
    expect(wrapper.get('[aria-label="使用自动主题"]').attributes('aria-pressed')).toBe('true')
  })
})
