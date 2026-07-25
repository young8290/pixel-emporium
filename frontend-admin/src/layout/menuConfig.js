import dashboard from '@/assets/pixel/icons/icon-dashboard.png'
import box from '@/assets/pixel/icons/icon-box.png'
import category from '@/assets/pixel/icons/icon-category.png'
import order from '@/assets/pixel/icons/icon-order.png'
import user from '@/assets/pixel/icons/icon-user.png'
import history from '@/assets/pixel/icons/icon-history.png'
import settings from '@/assets/pixel/icons/icon-settings.png'
import star from '@/assets/pixel/icons/icon-star.png'

const menuConfig = [
  { path: '/dashboard', title: '仪表盘', icon: dashboard },
  // [gen-start]
  // [gen-end]
  { path: '/product', title: '商品管理', icon: box },
  { path: '/product-sku', title: 'SKU 与规格', icon: box },
  { path: '/category', title: '分类管理', icon: category },
  { path: '/order', title: '订单管理', icon: order },
  { path: '/review', title: '评论管理', icon: star },
  { path: '/user', title: '用户管理', icon: user },
  { path: '/log', title: '操作日志', icon: history },
  { path: '/mail-settings', title: '邮件设置', icon: settings, roles: ['SUPER_ADMIN'] }
]
export default menuConfig
