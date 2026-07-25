import http from '../http'

export const listReviews = (params = {}) =>
  http.get('/business/admin/review/list', { params })

export const hideReview = (id, reason) =>
  http.put(`/business/admin/review/${id}/hide`, { reason })

export const restoreReview = id =>
  http.put(`/business/admin/review/${id}/restore`)
