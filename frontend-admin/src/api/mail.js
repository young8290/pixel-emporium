import http from './http'
export const getMailConfig=()=>http.get('/user/admin/mail/config')
export const updateMailConfig=(payload)=>http.put('/user/admin/mail/config',payload)
export const testMailConnection=()=>http.post('/user/admin/mail/test-connection',{})
export const sendMailTest=(payload)=>http.post('/user/admin/mail/test-send',payload)
export const getMailPreview=(templateKey)=>http.get('/user/admin/mail/preview',{params:{templateKey},responseType:'text'})
export const listMailDeliveries=(params)=>http.get('/user/admin/mail/delivery/list',{params})
export const retryMailDelivery=(id)=>http.post(`/user/admin/mail/delivery/${id}/retry`)
