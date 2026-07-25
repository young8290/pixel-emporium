import http from './http'

export const getDashboardStatistics = () => http.get('/business/admin/dashboard/statistics')
