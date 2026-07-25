import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProfileView from '../ProfileView.vue'

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  changePassword: vi.fn(),
  clearUserInfo: vi.fn(),
  replace: vi.fn(),
  success: vi.fn(),
  error: vi.fn()
}))

vi.mock('@/api/profile', () => ({
  getProfile: mocks.getProfile,
  updateProfile: mocks.updateProfile,
  changePassword: mocks.changePassword
}))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ clearUserInfo: mocks.clearUserInfo }) }))
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }))
vi.mock('jsencrypt', () => ({
  default: class {
    setPublicKey() {}
    encrypt(value) { return `encrypted:${value}` }
  }
}))
vi.mock('@/components/ui/PixelToast', () => ({ default: { success: mocks.success, error: mocks.error } }))

const PixelInputStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">'
}
const PixelButtonStub = { template: '<button><slot /></button>' }

function mountView() {
  return mount(ProfileView, {
    global: {
      directives: { loading: () => {} },
      stubs: { PixelInput: PixelInputStub, PixelButton: PixelButtonStub }
    }
  })
}

describe('profile view', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getProfile.mockResolvedValue({
      email: 'buyer@example.com', name: '店主', mobile: '', avatar: ''
    })
  })

  it('loads and saves the current user profile', async () => {
    mocks.updateProfile.mockResolvedValue({
      email: 'buyer@example.com', name: '新店主', mobile: '', avatar: ''
    })
    const wrapper = mountView()
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs[1].setValue('新店主')
    await wrapper.findAll('form')[0].trigger('submit')
    await flushPromises()

    expect(mocks.updateProfile).toHaveBeenCalledWith(expect.objectContaining({ name: '新店主' }))
    expect(mocks.success).toHaveBeenCalledWith('个人资料已保存')
  })

  it('encrypts password fields and clears the revoked session', async () => {
    mocks.changePassword.mockResolvedValue(true)
    const wrapper = mountView()
    await flushPromises()
    const inputs = wrapper.findAll('input')
    await inputs[4].setValue('Old password 2026')
    await inputs[5].setValue('New password 2026!')
    await inputs[6].setValue('New password 2026!')
    await wrapper.findAll('form')[1].trigger('submit')
    await flushPromises()

    expect(mocks.changePassword).toHaveBeenCalledWith({
      currentPassword: 'encrypted:Old password 2026',
      newPassword: 'encrypted:New password 2026!'
    })
    expect(mocks.clearUserInfo).toHaveBeenCalled()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })
})
