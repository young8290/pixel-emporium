import http from './http'

export const listProductReviews = (productId, params = {}) =>
  http.get(`/business/product/${productId}/reviews`, { params })

export const getReviewSummary = (productId) =>
  http.get(`/business/product/${productId}/review-summary`)

export const createReview = (payload) => http.post('/business/review', payload)
export const updateReview = (id, payload) => http.put(`/business/review/${id}`, payload)
export const deleteReview = (id) => http.delete(`/business/review/${id}`)

export const getOrderReviewEligibility = (orderId) =>
  http.get(`/business/review/order/${orderId}`)
