import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductFavoriteButton from '../ProductFavoriteButton.vue'

const api = vi.hoisted(() => ({
  getFavoriteStatus: vi.fn(),
  addFavorite: vi.fn(),
  removeFavorite: vi.fn(),
}))
const user = vi.hoisted(() => ({ token: 'token' }))
const router = vi.hoisted(() => ({ push: vi.fn() }))
const route = vi.hoisted(() => ({ fullPath: '/product/7' }))

vi.mock('@/api/engagement', () => api)
vi.mock('@/stores/user', () => ({ useUserStore: () => user }))
vi.mock('vue-router', () => ({
  useRouter: () => router,
  useRoute: () => route,
}))

describe('ProductFavoriteButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    user.token = 'token'
    api.getFavoriteStatus.mockResolvedValue(false)
    api.addFavorite.mockResolvedValue(true)
    api.removeFavorite.mockResolvedValue(true)
  })

  it('loads status and toggles the current product favorite', async () => {
    const wrapper = mount(ProductFavoriteButton, { props: { productId: '7' } })
    await flushPromises()

    expect(api.getFavoriteStatus).toHaveBeenCalledWith('7')
    expect(wrapper.get('button').attributes('aria-pressed')).toBe('false')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(api.addFavorite).toHaveBeenCalledWith('7')
    expect(wrapper.get('button').attributes('aria-pressed')).toBe('true')
    expect(wrapper.text()).toContain('已收藏')
  })

  it('redirects guests to login without calling the API', async () => {
    user.token = ''
    const wrapper = mount(ProductFavoriteButton, { props: { productId: '7' } })
    await flushPromises()

    await wrapper.get('button').trigger('click')

    expect(router.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/product/7' } })
    expect(api.addFavorite).not.toHaveBeenCalled()
  })
})
