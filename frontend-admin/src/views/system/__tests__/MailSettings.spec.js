import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MailSettings from '../MailSettings.vue'

const mocks = vi.hoisted(() => ({ getMailConfig: vi.fn(), updateMailConfig: vi.fn(), listMailDeliveries: vi.fn(), getMailPreview: vi.fn(), testMailConnection: vi.fn(), sendMailTest: vi.fn(), retryMailDelivery: vi.fn() }))
vi.mock('@/api/mail', () => mocks)

describe('MailSettings', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getMailConfig.mockResolvedValue({ version: 4, passwordConfigured: true, securityMode: 'STARTTLS' })
    mocks.listMailDeliveries.mockResolvedValue({ list: [] })
    mocks.getMailPreview.mockResolvedValue('<p>preview</p>')
  })

  it('does not refill a configured password and saves the current version', async () => {
    const wrapper = mount(MailSettings, { global: { stubs: { 'el-form': true, 'el-input': true, 'el-button': true, 'el-select': true, 'el-option': true, 'el-switch': true, 'el-table': true, 'el-table-column': true } } })
    await Promise.resolve()
    await Promise.resolve()
    expect(wrapper.vm.form.password).toBe('')
    expect(wrapper.vm.form.version).toBe(4)

    await wrapper.vm.save()
    expect(mocks.updateMailConfig).toHaveBeenCalledWith(expect.objectContaining({ version: 4, password: '' }))
  })

  it('reloads the latest configuration after a version conflict', async () => {
    mocks.updateMailConfig.mockRejectedValue({ response: { status: 409 } })
    const wrapper = mount(MailSettings, { global: { stubs: { 'el-form': true, 'el-input': true, 'el-button': true, 'el-select': true, 'el-option': true, 'el-switch': true, 'el-table': true, 'el-table-column': true } } })
    await Promise.resolve()
    await wrapper.vm.save()

    expect(mocks.getMailConfig).toHaveBeenCalledTimes(2)
  })

  it('uses a sandboxed preview and exposes mail security modes with independent loading state', async () => {
    const wrapper = mount(MailSettings, { global: { stubs: { 'el-form': { template: '<form><slot/></form>' }, 'el-form-item': { template: '<div><slot/></div>' }, 'el-input': true, 'el-input-number': true, 'el-button': true, 'el-select': { template: '<select><slot/></select>' }, 'el-option': { template: '<option><slot/></option>' }, 'el-switch': true, 'el-table': true, 'el-table-column': true } } })
    await Promise.resolve()
    await Promise.resolve()
    expect(wrapper.find('iframe').attributes('sandbox')).toBe('')
    expect(wrapper.html()).toContain('STARTTLS')
    expect(wrapper.html()).toContain('SSL/TLS')
    expect(wrapper.vm.testing).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
    expect(wrapper.vm.loading).toBe(false)
  })

  it('exposes reply-to and timeout controls and sends delivery filters to the list API', async () => {
    const wrapper = mount(MailSettings, { global: { stubs: { 'el-form': { template: '<form><slot/></form>' }, 'el-form-item': { props: ['label'], template: '<label :data-label="label"><slot/></label>' }, 'el-input': true, 'el-input-number': true, 'el-button': true, 'el-select': true, 'el-option': true, 'el-switch': true, 'el-table': true, 'el-table-column': true } } })
    await Promise.resolve()
    await Promise.resolve()

    expect(wrapper.find('[data-label="回复地址"]').exists()).toBe(true)
    expect(wrapper.find('[data-label="连接超时 (ms)"]').exists()).toBe(true)
    expect(wrapper.find('[data-label="读取超时 (ms)"]').exists()).toBe(true)

    wrapper.vm.filters.recipient = 'buyer@example.com'
    wrapper.vm.filters.templateKey = 'ORDER_CREATED'
    wrapper.vm.filters.status = 'DEAD'
    wrapper.vm.filters.eventId = 'ORDER_CREATED:99'
    await wrapper.vm.loadRows()

    expect(mocks.listMailDeliveries).toHaveBeenLastCalledWith({
      page: 1,
      size: 50,
      recipient: 'buyer@example.com',
      templateKey: 'ORDER_CREATED',
      status: 'DEAD',
      eventId: 'ORDER_CREATED:99'
    })
  })

  it('shows retry only for dead deliveries and delegates the selected id', async () => {
    mocks.listMailDeliveries.mockResolvedValue({
      list: [
        { id: '77', status: 'DEAD' },
        { id: '78', status: 'RETRY' }
      ]
    })
    const wrapper = mount(MailSettings, {
      global: {
        stubs: {
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-input-number': true,
          'el-select': true,
          'el-option': true,
          'el-switch': true,
          'el-button': { template: '<button @click="$emit(\'click\')"><slot/></button>' },
          'el-table': { template: '<div><slot/></div>' },
          'el-table-column': {
            template: '<div><slot :row="{ id: \'77\', status: \'DEAD\' }"/></div>'
          }
        }
      }
    })
    await Promise.resolve()
    await Promise.resolve()

    const retryButton = wrapper.findAll('button').find(button => button.text() === '重试')
    expect(retryButton).toBeTruthy()
    await retryButton.trigger('click')

    expect(mocks.retryMailDelivery).toHaveBeenCalledWith('77')
  })
})
