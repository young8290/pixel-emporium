import {createRouter,createWebHistory} from 'vue-router'
import {useUserStore} from '@/stores/user'
import routes from './routes'
export const router=createRouter({history:createWebHistory(),routes,scrollBehavior:()=>({top:0})})
router.beforeEach(to=>{const user=useUserStore();document.title=`${to.meta.title||'管理后台'} · 像素百货`;if(to.meta.requiresAuth!==false&&!user.token)return{path:'/login',query:{redirect:to.fullPath}};if(to.meta.guestOnly&&user.token)return'/dashboard';const allowed=to.matched.every(record=>{const roles=record.meta.roles||[];return roles.length===0||roles.some(role=>user.roles.includes(role))});if(user.token&&!allowed)return'/403';return true})
