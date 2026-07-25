import http from '../http'
export const listStockLogs=(params={})=>http.get('/business/admin/stock-log/list',{params})
