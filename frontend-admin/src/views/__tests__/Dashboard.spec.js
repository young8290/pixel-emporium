import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from '../Dashboard.vue'

const mocks = vi.hoisted(() => ({ getDashboardStatistics: vi.fn() }))
vi.mock('@/api/dashboard', () => ({ getDashboardStatistics: mocks.getDashboardStatistics }))

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getDashboardStatistics.mockResolvedValue({
      todayOrderCount: 3,
      todayPaidSales: 88.8,
      totalOrderCount: 20,
      onSaleProductCount: 9,
      sevenDayTrend: [{ day: '2026-07-25', orderCount: 3, paidSales: 88.8 }],
      hotProducts: [{ productId: '7', productTitle: '像素种子', soldQuantity: 12, paidAmount: 36 }],
      recentOrders: []
    })
  })

  it('loads all dashboard sections from the dedicated aggregate API', async () => {
    const wrapper = mount(Dashboard, {
      global: {
        stubs: {
          StatCard: { props: ['label', 'value', 'hint'], template: '<div>{{ label }} {{ value }} {{ hint }}</div>' },
          SimpleChart: { props: ['values', 'labels'], template: '<div data-test="chart">{{ values.join(",") }} {{ labels.join(",") }}</div>' },
          StatusTag: true,
          'el-skeleton': true,
          'el-result': true,
          'el-button': true,
          'el-table': { template: '<div><slot /></div>' },
          'el-table-column': true
        }
      }
    })
    await flushPromises()

    expect(mocks.getDashboardStatistics).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('今日订单 3 累计 20')
    expect(wrapper.text()).toContain('今日已支付销售额 ¥88.80')
    expect(wrapper.get('[data-test="chart"]').text()).toContain('3 07-25')
    expect(wrapper.text()).not.toContain('近似聚合')
  })
})
