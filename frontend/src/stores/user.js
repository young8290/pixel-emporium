import {defineStore} from 'pinia'
import {ref} from 'vue'
import { decodeJwtPayload } from '@/utils/jwt'

export const useUserStore = defineStore('user', () => {
  const email = ref(localStorage.getItem('userEmail') || '')
  const token = ref(localStorage.getItem('userToken') || '')
  const roles = ref(JSON.parse(localStorage.getItem('userRoles') || '[]'))
  const permissions = ref(JSON.parse(localStorage.getItem('userPermissions') || '[]'))

  const setUserInfo = (userInfo) => {
    const payload = decodeJwtPayload(userInfo.token)
    email.value = userInfo.email || payload.email || ''
    token.value = userInfo.token || ''
    roles.value = Array.isArray(payload.roles) ? payload.roles : []
    permissions.value = Array.isArray(payload.permissions) ? payload.permissions : []

    localStorage.setItem('userEmail', email.value)
    localStorage.setItem('userToken', token.value)
    localStorage.setItem('userRoles', JSON.stringify(roles.value))
    localStorage.setItem('userPermissions', JSON.stringify(permissions.value))
  }

  const clearUserInfo = () => {
    email.value = ''
    token.value = ''
    roles.value = []
    permissions.value = []

    localStorage.removeItem('userEmail')
    localStorage.removeItem('userToken')
    localStorage.removeItem('userRoles')
    localStorage.removeItem('userPermissions')
  }

  return {
    email,
    token,
    roles,
    permissions,
    setUserInfo,
    clearUserInfo
  }
})
