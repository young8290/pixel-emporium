import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { decodeJwtPayload } from '@/utils/jwt'
export const useUserStore=defineStore('user',()=>{
  const email=ref(localStorage.getItem('adminEmail')||'')
  const token=ref(localStorage.getItem('adminToken')||'')
  const roles=ref(JSON.parse(localStorage.getItem('adminRoles')||'[]'))
  const isAdmin=computed(()=>roles.value.some(role=>['ADMIN','SUPER_ADMIN'].includes(role)))
  function setUserInfo(info){const payload=decodeJwtPayload(info.token);email.value=info.email||payload.email||'';token.value=info.token||'';roles.value=Array.isArray(payload.roles)?payload.roles:[];localStorage.setItem('adminEmail',email.value);localStorage.setItem('adminToken',token.value);localStorage.setItem('adminRoles',JSON.stringify(roles.value))}
  function clearUserInfo(){email.value='';token.value='';roles.value=[];localStorage.removeItem('adminEmail');localStorage.removeItem('adminToken');localStorage.removeItem('adminRoles')}
  return{email,token,roles,isAdmin,setUserInfo,clearUserInfo}
})
