import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AmbientParticles from '../AmbientParticles.vue'
import NightOverlay from '../NightOverlay.vue'
import ViewportFrame from '../ViewportFrame.vue'
import WallpaperLayer from '../WallpaperLayer.vue'
import { useThemeStore } from '@/stores/theme'

describe('ambient system', () => {
  let pinia
  let originalMatchMedia

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    originalMatchMedia = window.matchMedia
  })

  afterEach(() => {
    window.matchMedia = originalMatchMedia
  })

  it('renders the selected wallpaper layer', () => {
    useThemeStore().setSeason('winter')
    const wrapper = mount(WallpaperLayer, { global: { plugins: [pinia] } })

    expect(wrapper.get('[data-season="winter"]').exists()).toBe(true)
  })

  it('renders four corners, stable night-light strips, a top garland and a bottom baseboard', () => {
    const wrapper = mount(ViewportFrame, { global: { plugins: [pinia] } })

    expect(wrapper.findAll('.vframe__corner')).toHaveLength(4)
    expect(wrapper.findAll('.vframe__lights')).toHaveLength(4)
    expect(wrapper.find('.vframe__garland').exists()).toBe(true)
    expect(wrapper.find('.vframe__baseboard').exists()).toBe(true)
  })

  it('only renders the lantern at night', async () => {
    const store = useThemeStore()
    store.setMode('day')
    const wrapper = mount(NightOverlay, { global: { plugins: [pinia] } })
    expect(wrapper.find('[data-testid="lantern"]').exists()).toBe(false)

    store.setMode('night')
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="lantern"]').exists()).toBe(true)
  })

  it('does not render particles when reduced motion is requested', () => {
    window.matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })

    const wrapper = mount(AmbientParticles, { global: { plugins: [pinia] } })
    expect(wrapper.findAll('.ambient-particles__item')).toHaveLength(0)
  })
})
