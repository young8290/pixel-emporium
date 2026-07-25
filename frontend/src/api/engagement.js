import http from './http'

export const listFavorites = (params = {}) => http.get('/business/favorite/list', { params })
export const getFavoriteStatus = (productId) => http.get(`/business/favorite/${productId}/status`)
export const addFavorite = (productId) => http.post(`/business/favorite/${productId}`)
export const removeFavorite = (productId) => http.delete(`/business/favorite/${productId}`)

export const listBrowseHistory = (params = {}) => http.get('/business/browse-history/list', { params })
export const recordBrowseHistory = (productId) => http.post(`/business/browse-history/${productId}`)
export const removeBrowseHistory = (productId) => http.delete(`/business/browse-history/${productId}`)
export const clearBrowseHistory = () => http.delete('/business/browse-history/clear')
