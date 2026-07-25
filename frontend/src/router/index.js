import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import routes from './routes'

export const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  const user = useUserStore()
  document.title = `${to.meta.title || '像素百货'} · 像素百货`
  if (to.meta.requiresAuth && !user.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.guestOnly && user.token) return '/'
  return true
})
