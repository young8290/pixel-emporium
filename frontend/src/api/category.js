import http from './http'

export const getCategoryTree = () => http.get('/business/category/tree')
