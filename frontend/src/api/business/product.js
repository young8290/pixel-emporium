import http from '../http.js'

const MODULE_PREFIX = '/business/product'

export function listProduct(params = {}) {
  const { page = 1, size = 10, keyword, onSale } = params

  return http.get(MODULE_PREFIX + '/list', {
    params: {
      page,
      size,
      keyword,
      onSale
    }
  })
}

export function saveProduct(data) {
  return http.post(MODULE_PREFIX + '/save', data)
}

export function deleteProduct(id) {
  return http.delete(MODULE_PREFIX + '/' + id)
}
