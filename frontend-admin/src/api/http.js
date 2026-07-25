import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { router } from '@/router'
const http=axios.create({baseURL:import.meta.env.VITE_API_BASE_URL,timeout:10000})
http.interceptors.request.use(config=>{const token=useUserStore().token;if(token)config.headers.Authorization=`Bearer ${token}`;return config})
http.interceptors.response.use(response=>{if(response.config.responseType==='text')return response.data;const body=response.data;if(body?.code===200)return body.data;const error=new Error(body?.msg||'请求未完成');ElMessage.error(error.message);return Promise.reject(error)},async error=>{const status=error.response?.status,message=error.response?.data?.msg;if(status===401){useUserStore().clearUserInfo();if(router.currentRoute.value.path!=='/login')await router.replace({path:'/login',query:{redirect:router.currentRoute.value.fullPath}})}else if(status===403&&router.currentRoute.value.path!=='/403'){await router.replace('/403')}ElMessage.error(message||(status>=500?'服务暂时不可用':'网络连接失败'));return Promise.reject(error)})
export default http
