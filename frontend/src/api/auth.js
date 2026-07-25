import http from './http'

export const login = (email, password) => http.post('/user/auth/login', { email, password })
export const register = (payload) => http.post('/user/auth/register', payload)
export const sendRegistrationCode = (email) => http.post('/user/auth/email-code', { email })
export const logout = () => http.post('/user/auth/logout')
export const refresh = () => http.post('/user/auth/refresh')
