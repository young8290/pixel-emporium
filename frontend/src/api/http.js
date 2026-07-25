import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { router } from '@/router'
import PixelToast from '@/components/ui/PixelToast'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = useUserStore().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (response.config.rawResponse) return envelope
    if (envelope?.code === 200) return envelope.data
    const error = new Error(envelope?.msg || '请求未完成')
    error.code = envelope?.code
    error.handled = true
    PixelToast.error(error.message)
    return Promise.reject(error)
  },
  async (error) => {
    const status = error.response?.status
    const message = error.response?.data?.msg
    if (status === 401) {
      const redirect = router.currentRoute.value.fullPath
      useUserStore().clearUserInfo()
      if (router.currentRoute.value.path !== '/login') {
        await router.push({ path: '/login', query: { redirect } })
      }
      PixelToast.warning(message || '登录状态已失效，请重新登录')
    } else if (status === 403) {
      PixelToast.error(message || '没有权限执行此操作')
      if (router.currentRoute.value.path !== '/403') await router.push('/403')
    } else if (status === 422) {
      PixelToast.warning(message || '请检查填写内容')
    } else if (status >= 500) {
      PixelToast.error('商店暂时忙不过来，请稍后重试')
    } else if (!error.handled) {
      PixelToast.error(message || (error.code === 'ECONNABORTED' ? '请求超时，请重试' : '网络连接失败'))
    }
    return Promise.reject(error)
  }
)

export const rawRequest = (config) => http({ ...config, rawResponse: true })
export default http
