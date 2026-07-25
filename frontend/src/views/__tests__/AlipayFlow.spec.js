import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PayView from '../PayView.vue'
import PayResultView from '../PayResultView.vue'

const api = vi.hoisted(() => ({
  createAlipayPayment: vi.fn(),
  getPaymentStatus: vi.fn(),
  queryAlipayPayment: vi.fn()
}))
const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }))
const route = vi.hoisted(() => ({ query: { orderNo: 'ORDER1' } }))
const submitAlipayForm = vi.hoisted(() => vi.fn())

vi.mock('@/api/pay', () => api)
vi.mock('@/utils/alipay', () => ({ submitAlipayForm }))
vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router
}))

describe('Alipay user flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    route.query.orderNo = 'ORDER1'
    api.createAlipayPayment.mockResolvedValue({
      payNo: 'PAY1',
      orderNo: 'ORDER1',
      amount: 12,
      formHtml: '<form action="https://openapi-sandbox.dl.alipaydev.com/gateway.do"></form>'
    })
    api.getPaymentStatus.mockResolvedValue({
      payNo: 'PAY1',
      orderNo: 'ORDER1',
      amount: 12,
      status: 'PENDING',
      orderStatus: 'PENDING_PAYMENT'
    })
  })

  it('creates an Alipay sandbox payment and submits its signed form', async () => {
    const wrapper = mountPayView()
    await flushPromises()

    expect(api.createAlipayPayment).toHaveBeenCalledWith('ORDER1')
    expect(wrapper.text()).toContain('支付宝沙箱收银台')
    expect(wrapper.text()).not.toContain('确认模拟支付')

    await wrapper.vm.launch()
    expect(submitAlipayForm).toHaveBeenCalledWith(expect.stringContaining('alipaydev.com'))
  })

  it('actively queries Alipay from the result page and renders success', async () => {
    api.queryAlipayPayment.mockResolvedValue({
      payNo: 'PAY1',
      orderNo: 'ORDER1',
      amount: 12,
      status: 'SUCCESS',
      orderStatus: 'PAID'
    })
    const wrapper = mountPayResult()
    await flushPromises()

    await wrapper.vm.queryAlipay()
    await flushPromises()

    expect(api.queryAlipayPayment).toHaveBeenCalledWith('ORDER1')
    expect(wrapper.vm.payment.status).toBe('SUCCESS')
    expect(wrapper.vm.waiting).toBe(false)
  })
})

function mountPayView() {
  return mount(PayView, {
    global: {
      stubs: {
        PixelButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' },
        PixelErrorState: true,
        PixelLoading: true
      }
    }
  })
}

function mountPayResult() {
  return mount(PayResultView, {
    global: {
      stubs: {
        PayStatusCard: { props: ['success'], template: '<section><slot/></section>' },
        PixelButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' },
        PixelErrorState: true,
        PixelLoading: true
      }
    }
  })
}
