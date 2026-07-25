import http from '../http'
export const listImages=(productId)=>http.get('/business/admin/product-image/list',{params:{productId}})
export const uploadImage=(form)=>http.post('/business/admin/product-image/upload',form,{headers:{'Content-Type':'multipart/form-data'}})
export const deleteImage=(id)=>http.delete(`/business/admin/product-image/${id}`)
export const setMainImage=(id)=>http.post(`/business/admin/product-image/${id}/set-main`)
export const stylizeImage=(id,presetKey)=>http.post(`/business/admin/product-image/${id}/stylize`,{presetKey})
export const styleStatus=(id)=>http.get(`/business/admin/product-image/${id}/style-status`)
