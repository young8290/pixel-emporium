import http from './http'

export const createAlipayPayment = (orderNo) =>
  http.post('/business/pay/alipay/create', { orderNo })
export const queryAlipayPayment = (orderNo) =>
  http.post('/business/pay/alipay/query', null, { params: { orderNo } })
export const getPaymentStatus = (orderNo) =>
  http.get('/business/pay/status', { params: { orderNo } })
