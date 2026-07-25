import http from './http'

export const listProducts = (params = {}) => http.get('/business/product/list', { params })
export const getProductSuggestions = (keyword, limit = 10) => http.get('/business/product/suggestions', { params: { keyword, limit } })
export const getProductRecommendations = (productId, limit = 8) => http.get(`/business/product/${productId}/recommendations`, { params: { limit } })

export const PREVIEW_PRODUCT_ID = '9000001'

const previewProduct = Object.freeze({
  product: {
    id: PREVIEW_PRODUCT_ID,
    title: '月光黄铜阅读灯',
    description: '一盏为夜读准备的小灯。黄铜灯罩收住光线，胡桃木底座让桌面保持安静温暖。\n\n这是本地开发环境的演示商品，用来查看商品详情、规格选择与购买区的完整样式。',
    onSale: true,
    rating: 4.9,
    soldCount: 128,
    reviewCount: 36,
    price: 88,
    categoryName: '家居照明'
  },
  attributes: [
    {
      id: '9000011',
      name: '灯光',
      values: [
        { id: '9000111', value: '暖光' },
        { id: '9000112', value: '柔白光' }
      ]
    },
    {
      id: '9000012',
      name: '尺寸',
      values: [
        { id: '9000121', value: '小号' },
        { id: '9000122', value: '大号' }
      ]
    }
  ],
  skus: [
    { id: '9000101', price: 88, stock: 24, skuCode: 'LAMP-WARM-S', attributeValueIds: ['9000111', '9000121'] },
    { id: '9000102', price: 108, stock: 18, skuCode: 'LAMP-WARM-L', attributeValueIds: ['9000111', '9000122'] },
    { id: '9000103', price: 92, stock: 16, skuCode: 'LAMP-WHITE-S', attributeValueIds: ['9000112', '9000121'] },
    { id: '9000104', price: 112, stock: 0, skuCode: 'LAMP-WHITE-L', attributeValueIds: ['9000112', '9000122'] }
  ],
  images: []
})

export const getProduct = (id) => {
  if (import.meta.env.DEV && String(id) === PREVIEW_PRODUCT_ID) return Promise.resolve(previewProduct)
  return http.get(`/business/product/${id}`)
}
