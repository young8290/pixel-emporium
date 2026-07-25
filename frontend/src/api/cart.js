import http from './http'

export const getCart = () => http.get('/business/cart')
export const addCartItem = (productSkuId, quantity = 1) =>
  http.post('/business/cart/add', { productSkuId, quantity })
export const updateCartItem = (payload) => http.put('/business/cart/update', payload)
export const removeCartItem = (id) => http.delete(`/business/cart/${id}`)
export const removeCartItems = (ids) => http.delete('/business/cart/batch', { data: ids })
export const clearCart = () => http.delete('/business/cart/clear')
