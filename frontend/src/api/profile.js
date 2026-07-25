import http from './http'

export const getProfile = () => http.get('/user/profile')
export const updateProfile = (payload) => http.put('/user/profile', payload)
export const changePassword = (payload) => http.put('/user/profile/password', payload)
