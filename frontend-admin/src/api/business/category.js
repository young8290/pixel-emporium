import http from '../http'
export const listCategories=(params={page:1,size:100})=>http.get('/business/admin/category/list',{params})
export const categoryTree=()=>http.get('/business/category/tree')
export const saveCategory=(data)=>http.post('/business/admin/category/save',data)
export const deleteCategory=(id)=>http.delete(`/business/admin/category/${id}`)
