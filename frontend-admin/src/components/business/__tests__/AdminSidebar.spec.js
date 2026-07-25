import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import AdminSidebar from '../AdminSidebar.vue'

describe('AdminSidebar role filtering', () => {
  it('hides mail settings from ADMIN and shows it to SUPER_ADMIN', () => {
    expect(mountWithRoles(['ADMIN']).text()).not.toContain('邮件设置')
    expect(mountWithRoles(['SUPER_ADMIN']).text()).toContain('邮件设置')
  })
})

function mountWithRoles(roles) {
  localStorage.setItem('adminRoles', JSON.stringify(roles))
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(AdminSidebar, {
    props: { collapsed: false },
    global: { plugins: [pinia], stubs: { RouterLink: { template: '<a><slot/></a>' } } }
  })
}
