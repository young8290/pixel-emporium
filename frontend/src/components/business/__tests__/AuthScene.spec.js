import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import AuthScene from '../AuthScene.vue'
import { useThemeStore } from '@/stores/theme'

describe('AuthScene', () => {
  let pinia

  beforeEach(() => {
    localStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
  })

  it.each([
    ['day', 'auth-scene-day'],
    ['night', 'auth-scene-night']
  ])('uses the %s scene without cropping it', (mode, imageName) => {
    useThemeStore().setMode(mode)
    const wrapper = mount(AuthScene, { global: { plugins: [pinia] } })
    const backdrop = wrapper.get('.auth-scene__backdrop')

    expect(backdrop.attributes('src')).toContain(imageName)
    expect(backdrop.classes()).toContain('auth-scene__backdrop--complete')
  })
})
