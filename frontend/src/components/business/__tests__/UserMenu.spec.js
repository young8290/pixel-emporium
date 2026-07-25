import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@/layout/ShopLayout.vue', () => ({ default: { template: '<main />' } }))
vi.mock('@/layout/UserCenterLayout.vue', () => ({ default: { template: '<main />' } }))

import UserMenu from '../UserMenu.vue'
import routes from '@/router/routes'

describe('favorite and history navigation', () => {
  it('registers the user center routes', () => {
    const userRoutes = routes[2].children.find((route) => route.path === 'user').children

    expect(userRoutes).toEqual(expect.arrayContaining([
      expect.objectContaining({ path: 'favorites', name: 'UserFavorites' }),
      expect.objectContaining({ path: 'history', name: 'UserBrowseHistory' }),
    ]))
  })

  it('shows favorite and history links in the user menu', () => {
    const wrapper = mount(UserMenu, {
      global: {
        stubs: {
          RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        },
      },
    })

    expect(wrapper.get('a[href="/user/favorites"]').text()).toContain('我的收藏')
    expect(wrapper.get('a[href="/user/history"]').text()).toContain('浏览历史')
  })
})
