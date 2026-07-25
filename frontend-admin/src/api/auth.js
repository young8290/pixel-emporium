import http from './http'
export const adminLogin=(email,password)=>http.post('/user/auth/admin/login',{email,password})
export const logout=()=>http.post('/user/auth/logout')
