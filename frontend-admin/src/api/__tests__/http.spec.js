import { describe, expect, it, vi } from 'vitest'

const axios = vi.hoisted(() => ({ create: vi.fn() }))
const client = vi.hoisted(() => ({
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() }
  }
}))

vi.mock('axios', () => ({ default: axios }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ token: '' }) }))
vi.mock('@/router', () => ({ router: { currentRoute: { value: { path: '/' } }, replace: vi.fn() } }))

describe('HTTP response interceptor', () => {
  it('returns explicit text responses without requiring the JSON API envelope', async () => {
    axios.create.mockReturnValue(client)
    await import('../http')
    const onFulfilled = client.interceptors.response.use.mock.calls[0][0]

    expect(onFulfilled({ config: { responseType: 'text' }, data: '<html>mail preview</html>' }))
      .toBe('<html>mail preview</html>')
  })
})
