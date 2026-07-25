import http from '../http'
export const listProducts=(params={})=>http.get('/business/admin/product/list',{params})
export const saveProduct=(data)=>http.post('/business/admin/product/save',data)
export const deleteProduct=(id)=>http.delete(`/business/admin/product/${id}`)
