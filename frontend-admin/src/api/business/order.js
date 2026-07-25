import http from '../http'
export const listOrders=(params={})=>http.get('/business/admin/order/list',{params})
export const getOrder=(id)=>http.get(`/business/admin/order/${id}`)
export const shipOrder=(id, payload)=>http.put(`/business/admin/order/${id}/ship`, payload)
