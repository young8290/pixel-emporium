import http from './http'
export const listUsers=(params={})=>http.get('/user/admin/user/list',{params})
export const getUser=(id)=>http.get(`/user/admin/user/${id}`)
export const updateEnabled=(id,enabled)=>http.put(`/user/admin/user/${id}/enabled`,{enabled})
export const replaceRoles=(id,roleIds)=>http.put(`/user/admin/user/${id}/roles`,{roleIds})
