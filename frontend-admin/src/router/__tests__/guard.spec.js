import { beforeEach, describe, expect, it, vi } from 'vitest'

const user = vi.hoisted(() => ({ token: '', roles: [] }))

vi.mock('@/stores/user', () => ({ useUserStore: () => user }))

describe('admin router role guard', () => {
  let router

  beforeEach(async () => {
    window.scrollTo = vi.fn()
    user.token = ''
    user.roles = []
    vi.resetModules()
    router = (await import('../index')).router
    await router.push('/login')
    await router.isReady()
  })

  it('redirects ADMIN away from the nested super-admin mail-settings record', async () => {
    user.token = 'admin-token'
    user.roles = ['ADMIN']

    await router.push('/mail-settings')

    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('allows SUPER_ADMIN through every matched record for mail-settings', async () => {
    user.token = 'super-admin-token'
    user.roles = ['SUPER_ADMIN']

    await router.push('/mail-settings')

    expect(router.currentRoute.value.path).toBe('/mail-settings')
  })
})
