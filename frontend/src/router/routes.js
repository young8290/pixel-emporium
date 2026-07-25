import ShopLayout from '@/layout/ShopLayout.vue'
import UserCenterLayout from '@/layout/UserCenterLayout.vue'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录', requiresAuth: false, guestOnly: true, zone: 'auth' } },
  { path: '/register', name: 'Register', component: () => import('@/views/RegisterView.vue'), meta: { title: '注册', requiresAuth: false, guestOnly: true, zone: 'auth' } },
  {
    path: '/',
    component: ShopLayout,
    meta: { requiresAuth: false },
    children: [
      { path: '', name: 'Home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页', requiresAuth: false, zone: 'shop' } },
      { path: 'products', name: 'Products', component: () => import('@/views/ProductListView.vue'), meta: { title: '全部商品', requiresAuth: false, zone: 'shop' } },
      { path: 'search', name: 'Search', component: () => import('@/views/SearchView.vue'), meta: { title: '搜索', requiresAuth: false, zone: 'shop' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetailView.vue'), meta: { title: '商品详情', requiresAuth: false, zone: 'shop' } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/CartView.vue'), meta: { title: '购物篮', requiresAuth: true, zone: 'funnel' } },
      { path: 'checkout', name: 'Checkout', component: () => import('@/views/CheckoutView.vue'), meta: { title: '确认订单', requiresAuth: true, zone: 'funnel' } },
      { path: 'pay', name: 'Pay', component: () => import('@/views/PayView.vue'), meta: { title: '支付宝支付', requiresAuth: true, zone: 'funnel' } },
      { path: 'pay/result', name: 'PayResult', component: () => import('@/views/PayResultView.vue'), meta: { title: '支付结果', requiresAuth: true, zone: 'funnel' } },
      {
        path: 'user',
        component: UserCenterLayout,
        meta: { title: '用户中心', requiresAuth: true, zone: 'user' },
        children: [
          { path: '', name: 'UserOverview', component: () => import('@/views/user/OverviewView.vue') },
          { path: 'orders', name: 'UserOrders', component: () => import('@/views/user/OrdersView.vue') },
          { path: 'orders/:id', name: 'UserOrderDetail', component: () => import('@/views/user/OrderDetailView.vue') },
          { path: 'address', name: 'UserAddress', component: () => import('@/views/user/AddressView.vue') },
          { path: 'favorites', name: 'UserFavorites', component: () => import('@/views/user/FavoritesView.vue') },
          { path: 'history', name: 'UserBrowseHistory', component: () => import('@/views/user/BrowseHistoryView.vue') },
          { path: 'profile', name: 'UserProfile', component: () => import('@/views/user/ProfileView.vue') }
        ]
      },
      { path: '403', name: 'Forbidden', component: () => import('@/views/error/Forbidden.vue'), meta: { title: '无权限', requiresAuth: false } },
      { path: 'network-error', name: 'NetworkError', component: () => import('@/views/error/NetworkError.vue'), meta: { title: '网络异常', requiresAuth: false } },
      { path: ':pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFound.vue'), meta: { title: '页面不存在', requiresAuth: false } }
    ]
  }
]

export default routes
