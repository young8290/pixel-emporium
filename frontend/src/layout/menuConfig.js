import {
  Monitor,
  Files,
  CoffeeCup,
  Goods,
} from '@element-plus/icons-vue'

const menuConfig = [
  {
    path: '/dashboard',
    title: '仪表盘',
    icon: Monitor
  },
  {
    path: '/sb',
    title: 'sb management',
    icon: Monitor
  },
  {
    path: '/Address',
    title: 'Address Management',
    icon: Monitor
  },
  {
    path: '/product',
    title: '商品管理',
    icon: Goods
  },
  {
    path: '/product-sku',
    title: '商品SKU',
    icon: Goods
  },
  {
    title: '多级菜单',
    icon: Files,
    children: [
      {
        path: '/empty',
        title: '空白页面',
        icon: CoffeeCup
      }
    ]
  }
]

export default menuConfig
