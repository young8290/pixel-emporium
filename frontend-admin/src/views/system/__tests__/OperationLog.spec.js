import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OperationLog from '../OperationLog.vue'

const mocks = vi.hoisted(() => ({
  listBusinessOperationLogs: vi.fn(),
  listUserOperationLogs: vi.fn()
}))

vi.mock('@/api/operationLog', () => mocks)

describe('OperationLog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.listBusinessOperationLogs.mockResolvedValue({ list: [{ action: 'PRODUCT_SAVE' }], total: 1 })
    mocks.listUserOperationLogs.mockResolvedValue({ list: [{ action: 'MAIL_CONFIG_UPDATE' }], total: 1 })
  })

  it('switches between business and user audit sources', async () => {
    const wrapper = mount(OperationLog, {
      global: {
        stubs: {
          FilterBar: { template: '<div><slot/><slot name="actions"/></div>' },
          'el-select': true,
          'el-option': true,
          'el-input': true,
          'el-button': true,
          'el-table': true,
          'el-table-column': true,
          'el-pagination': true
        }
      }
    })
    await Promise.resolve()
    await Promise.resolve()

    expect(mocks.listBusinessOperationLogs).toHaveBeenCalled()
    expect(wrapper.vm.rows[0].action).toBe('PRODUCT_SAVE')

    wrapper.vm.source = 'user'
    await wrapper.vm.load()

    expect(mocks.listUserOperationLogs).toHaveBeenCalled()
    expect(wrapper.vm.rows[0].action).toBe('MAIL_CONFIG_UPDATE')
  })
})
