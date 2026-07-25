import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserAdmin from '../UserAdmin.vue'

const api = vi.hoisted(() => ({
  listUsers: vi.fn(),
  replaceRoles: vi.fn(),
  updateEnabled: vi.fn()
}))

vi.mock('@/api/user', () => api)

describe('UserAdmin role management', () => {
  beforeEach(() => {
    localStorage.clear()
    api.listUsers.mockResolvedValue({ list: [], total: 0 })
  })

  it('only exposes role management to super administrators', () => {
    const admin = mountWithRoles(['ADMIN'])
    expect(admin.vm.canManageRoles).toBe(false)

    const superAdmin = mountWithRoles(['SUPER_ADMIN'])
    expect(superAdmin.vm.canManageRoles).toBe(true)
  })
})

function mountWithRoles(roles) {
  localStorage.setItem('adminRoles', JSON.stringify(roles))
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(UserAdmin, {
    global: {
      plugins: [pinia],
      stubs: {
        FilterBar: { template: '<div><slot/><slot name="actions"/></div>' },
        'el-input': true,
        'el-select': true,
        'el-option': true,
        'el-button': true,
        'el-table': true,
        'el-table-column': true,
        'el-switch': true,
        'el-tag': true,
        'el-pagination': true,
        'el-dialog': true,
        'el-checkbox-group': true,
        'el-checkbox': true
      }
    }
  })
}
