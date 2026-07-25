import http from './http'

export const listAddresses = (params = { page: 1, size: 100 }) =>
  http.get('/user/address/list', { params })
export const saveAddress = (payload) => http.post('/user/address/save', payload)
export const deleteAddress = (id) => http.delete(`/user/address/${id}`)
export const setDefaultAddress = (id) => http.put(`/user/address/default/${id}`)
