import http from './http'
export const listBusinessOperationLogs=(params={})=>http.get('/business/admin/operation-log/list',{params})
export const listUserOperationLogs=(params={})=>http.get('/user/admin/operation-log/list',{params})
