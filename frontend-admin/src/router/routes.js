import AppLayout from '@/layout/AppLayout.vue'
const routes=[
  {path:'/login',name:'Login',component:()=>import('@/views/LoginView.vue'),meta:{title:'管理员登录',requiresAuth:false,guestOnly:true}},
  {path:'/',component:AppLayout,redirect:'/dashboard',meta:{requiresAuth:true,roles:['ADMIN','SUPER_ADMIN']},children:[
    {path:'dashboard',name:'Dashboard',component:()=>import('@/views/Dashboard.vue'),meta:{title:'仪表盘'}},
    // [gen-start]
    // [gen-end]
    {path:'product',name:'Product',component:()=>import('@/views/business/Product.vue'),meta:{title:'商品管理'}},
    {path:'product-sku',name:'ProductSku',component:()=>import('@/views/business/ProductSku.vue'),meta:{title:'SKU 与规格'}},
    {path:'category',name:'Category',component:()=>import('@/views/business/Category.vue'),meta:{title:'分类管理'}},
    {path:'order',name:'Order',component:()=>import('@/views/business/Order.vue'),meta:{title:'订单管理'}},
    {path:'order/:id',name:'OrderDetail',component:()=>import('@/views/business/OrderDetail.vue'),meta:{title:'订单详情'}},
    {path:'review',name:'Review',component:()=>import('@/views/business/Review.vue'),meta:{title:'评论管理',roles:['ADMIN','SUPER_ADMIN']}},
    {path:'user',name:'UserAdmin',component:()=>import('@/views/user/UserAdmin.vue'),meta:{title:'用户管理'}},
    {path:'log',name:'OperationLog',component:()=>import('@/views/system/OperationLog.vue'),meta:{title:'操作日志'}},
    {path:'mail-settings',name:'MailSettings',component:()=>import('@/views/system/MailSettings.vue'),meta:{title:'邮件设置',roles:['SUPER_ADMIN']}},
    {path:'403',name:'Forbidden',component:()=>import('@/views/error/Forbidden.vue'),meta:{title:'无权限',requiresAuth:false}},
    {path:':pathMatch(.*)*',name:'NotFound',component:()=>import('@/views/error/NotFound.vue'),meta:{title:'页面不存在',requiresAuth:false}}
  ]}
]
export default routes
