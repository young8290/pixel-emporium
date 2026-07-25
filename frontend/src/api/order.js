import http from './http'

export const createOrder = (payload) => http.post('/business/order/create', payload)
export const listOrders = (params = {}) => http.get('/business/order/list', { params })
export const getOrder = (id) => http.get(`/business/order/${id}`)
export const cancelOrder = (id) => http.put(`/business/order/${id}/cancel`)
export const confirmOrder = (id) => http.put(`/business/order/${id}/confirm`)
