import { flushPromises, mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductDetailView from '../ProductDetailView.vue'

const api = vi.hoisted(() => ({ getProduct: vi.fn(), getProductRecommendations: vi.fn() }))
const engagement = vi.hoisted(() => ({ recordBrowseHistory: vi.fn() }))
const user = vi.hoisted(() => ({ token: '' }))
const route = vi.hoisted(() => ({ params: { id: '301' }, fullPath: '/product/301' }))
const router = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn() }))

vi.mock('@/api/product', () => api)
vi.mock('@/api/engagement', () => engagement)
vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router,
  createWebHistory: () => ({}),
  createRouter: () => ({
    beforeEach: vi.fn(),
    currentRoute: { value: { path: '/', fullPath: '/' } },
    push: vi.fn()
  })
}))
vi.mock('@/stores/cart', () => ({ useCartStore: () => ({ add: vi.fn() }) }))
vi.mock('@/stores/user', () => ({ useUserStore: () => user }))

const detail = (overrides = {}, skuOverrides = {}) => ({
  product: {
    id: '301', title: '像素键盘', description: '木质键盘', price: 88,
    rating: 0, reviewCount: 0, soldCount: 20, ...overrides
  },
  images: [], attributes: [], skus: [{ id: '701', price: 88, stock: 2, ...skuOverrides }]
})

const stubs = {
  ProductReviews: { template: '<section data-test="product-reviews" />' },
  ProductRecommendations: { props: ['productId'], template: '<section data-test="recommendations">{{ productId }}</section>' },
  PixelSkeleton: { template: '<div />' },
  PixelErrorState: { template: '<div />' },
  Gallery: { template: '<div />' },
  PriceBlock: {
    props: ['price', 'original'],
    template: '<div data-test="price-block" :data-price="price" :data-original="original" />'
  },
  ProductFavoriteButton: { template: '<button data-test="favorite-button" />' },
  SkuSelector: { props: ['modelValue', 'attributes', 'skus'], template: '<div />' },
  QtyStepper: { template: '<div />' },
  StockHint: { template: '<div />' },
  AddCartBar: { template: '<button data-test="buy-bar" />' }
}

describe('ProductDetailView reviews', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    user.token = ''
    api.getProduct.mockResolvedValue(detail())
    engagement.recordBrowseHistory.mockResolvedValue(true)
  })

  it('places the review section after the description and does not invent a rating', async () => {
    const wrapper = mount(ProductDetailView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.find('[data-test="product-reviews"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无评分')
    expect(wrapper.text()).not.toContain('★ 5.0')
    expect(wrapper.find('.description').element.nextElementSibling.matches('[data-test="product-reviews"]')).toBe(true)
  })

  it('keeps the purchase bar mounted when the review area has a request failure', async () => {
    const wrapper = mount(ProductDetailView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.find('[data-test="buy-bar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="product-reviews"]').exists()).toBe(true)
  })

  it('passes the selected SKU original price to the price block', async () => {
    api.getProduct.mockResolvedValue(detail({}, { price: 80, originalPrice: 100 }))

    const wrapper = mount(ProductDetailView, { global: { stubs } })
    await flushPromises()

    expect(wrapper.find('[data-test="price-block"]').attributes('data-price')).toBe('80')
    expect(wrapper.find('[data-test="price-block"]').attributes('data-original')).toBe('100')
  })

  it('records a browse and mounts favorite control for authenticated users', async () => {
    user.token = 'token'

    const wrapper = mount(ProductDetailView, { global: { stubs } })
    await flushPromises()

    expect(engagement.recordBrowseHistory).toHaveBeenCalledWith('301')
    expect(wrapper.find('[data-test="favorite-button"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="recommendations"]').text()).toBe('301')
  })

  it('allows mobile detail grid items to shrink below long unbroken content', () => {
    const source = readFileSync('src/views/ProductDetailView.vue', 'utf8')

    expect(source).toContain('@media(max-width:800px){.detail-main{grid-template-columns:minmax(0,1fr)}')
    expect(source).toContain('.detail-main>*{min-width:0}')
    expect(source).toContain('.detail-info>*{min-width:0}')
    expect(source).toMatch(/\.detail-info h1\{[^}]*overflow-wrap:anywhere/)
  })
})
