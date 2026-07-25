import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthScene from '@/components/business/AuthScene.vue'
import LoginView from '../LoginView.vue'
import RegisterView from '../RegisterView.vue'

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  sendRegistrationCode: vi.fn(),
  replace: vi.fn(),
  setUserInfo: vi.fn(),
  success: vi.fn(),
  error: vi.fn()
}))

vi.mock('@/api/auth', () => ({ login: mocks.login, register: mocks.register, sendRegistrationCode: mocks.sendRegistrationCode }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ setUserInfo: mocks.setUserInfo }) }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: mocks.replace })
}))
vi.mock('jsencrypt', () => ({
  default: class {
    setPublicKey() {}
    encrypt(password) { return `encrypted:${password}` }
  }
}))
vi.mock('@/components/ui/PixelToast', () => ({
  default: { success: mocks.success, error: mocks.error }
}))

const PixelInputStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">'
}
const PixelButtonStub = { template: '<button><slot /></button>' }
const RouterLinkStub = { template: '<a><slot /></a>' }

function mountView(component) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(component, {
    global: {
      plugins: [pinia],
      stubs: {
        PixelInput: PixelInputStub,
        PixelButton: PixelButtonStub,
        RouterLink: RouterLinkStub
      }
    }
  })
}

describe('auth views', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('keeps encrypted login submission inside the shared plaque scene', async () => {
    mocks.login.mockResolvedValue({ email: 'shop@example.com', token: 'token' })
    const wrapper = mountView(LoginView)

    expect(wrapper.findComponent(AuthScene).exists()).toBe(true)
    expect(wrapper.find('.auth-card--plaque').exists()).toBe(true)
    expect(wrapper.find('.auth-submit--wood').exists()).toBe(true)

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('shop@example.com')
    await inputs[1].setValue('secret')
    await wrapper.get('form').trigger('submit')
    await nextTick()

    expect(mocks.login).toHaveBeenCalledWith('shop@example.com', 'encrypted:secret')
  })

  it('keeps encrypted registration submission inside the shared plaque scene', async () => {
    mocks.register.mockResolvedValue({})
    const wrapper = mountView(RegisterView)

    expect(wrapper.findComponent(AuthScene).exists()).toBe(true)
    expect(wrapper.find('.auth-card--plaque').exists()).toBe(true)
    expect(wrapper.find('.auth-submit--wood').exists()).toBe(true)

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('店主')
    await inputs[1].setValue('shop@example.com')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('')
    await inputs[4].setValue('Valid password 2026')
    await inputs[5].setValue('Valid password 2026')
    await wrapper.get('form').trigger('submit')
    await nextTick()

    expect(mocks.register).toHaveBeenCalledWith(expect.objectContaining({
      email: 'shop@example.com',
      name: '店主',
      emailCode: '123456',
      password: 'encrypted:Valid password 2026'
    }))
  })

  it('sends a registration code and includes the six digit code in the encrypted registration payload', async () => {
    mocks.sendRegistrationCode.mockResolvedValue({ cooldownSeconds: 60 })
    mocks.register.mockResolvedValue({})
    const wrapper = mountView(RegisterView)
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('店主')
    await inputs[1].setValue('shop@example.com')
    await wrapper.get('[data-test="send-email-code"]').trigger('click')
    expect(mocks.sendRegistrationCode).toHaveBeenCalledWith('shop@example.com')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('')
    await inputs[4].setValue('Valid password 2026')
    await inputs[5].setValue('Valid password 2026')
    await wrapper.get('form').trigger('submit')
    await nextTick()
    expect(mocks.register).toHaveBeenCalledWith(expect.objectContaining({ emailCode: '123456', password: 'encrypted:Valid password 2026' }))
  })
})
