<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="aside-container">
      <div class="logo-container">
        <img src="../assets/logo.svg" alt="Logo" class="logo">
        <span v-show="!isCollapse" class="logo-text">xxx App</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="$route.path"
          :collapse="isCollapse"
          background-color="#001529"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#fff"
          :unique-opened="true"
          router
        >
          <template v-for="item in menuConfig" :key="item.path || item.title">
            <el-menu-item v-if="!item.children" :index="item.path">
              <el-icon>
                <component :is="item.icon"/>
              </el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>

            <el-sub-menu v-else :index="item.title">
              <template #title>
                <el-icon>
                  <component :is="item.icon"/>
                </el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.path"
                :index="child.path"
              >
                <el-icon>
                  <component :is="child.icon"/>
                </el-icon>
                <span>{{ child.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
      <div class="aside-footer">
        <div
          class="collapse-trigger"
          @click="isCollapse = !isCollapse"
        >
          <el-icon>
            <Fold v-show="!isCollapse"/>
            <Expand v-show="isCollapse"/>
          </el-icon>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="fold-btn"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-show="!isCollapse"/>
            <Expand v-show="isCollapse"/>
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @visible-change="handleDropdownVisibleChange">
            <span class="user-info">
              <el-avatar :size="32"/>
              <span class="username">{{ userStore.email }}</span>
              <el-icon class="el-icon--right" :class="{ 'is-reverse': isDropdownVisible }">
                <arrow-down/>
              </el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon>
                    <User/>
                  </el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon>
                    <Key/>
                  </el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon>
                    <SwitchButton/>
                  </el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component"/>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue'
import {useRouter} from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  User,
  Key,
  SwitchButton,
  Fold,
  Expand,
  ArrowDown,
} from '@element-plus/icons-vue'
import menuConfig from './menuConfig.js'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const isDropdownVisible = ref(false)


defineOptions({
  name: 'AdminLayout'
})

// 监听窗口大小变化
const handleResize = () => {
  isCollapse.value = window.innerWidth <= 1024
}

// 组件挂载时添加监听器
onMounted(() => {
  handleResize() // 初始化时执行一次
  window.addEventListener('resize', handleResize)
})

// 组件卸载前移除监听器
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

const handleLogout = () => {
  // 清除用户信息
  userStore.clearUserInfo()
  // 跳转到登录页
  router.push('/login')
}

const handleDropdownVisibleChange = (visible) => {
  isDropdownVisible.value = visible
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside-container {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.logo-container {
  height: 60px;
  line-height: 60px;
  background: #002140;
  display: flex;
  align-items: center;
  padding-left: 16px;
  overflow: hidden;
}

.logo {
  width: 32px;
  height: 32px;
}

.logo-text {
  color: white;
  font-size: 16px;
  font-weight: 600;
  margin-left: 12px;
  white-space: nowrap;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
}

.header-left {
  display: flex;
  align-items: center;
}

.fold-btn {
  padding: 0 12px;
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.fold-btn:hover {
  color: #409EFF;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 8px;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #666;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
}

:deep(.el-sub-menu .el-sub-menu__title) {
  background-color: #001529 !important;
}

:deep(.el-menu-item) {
  background-color: #001529 !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: #1890ff !important;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media screen and (max-width: 768px) {
  .aside-container {
    position: fixed;
    height: 100vh;
    z-index: 1000;
  }

  .el-main {
    margin-left: 64px;
  }

  .logo-text {
    display: none;
  }

  .fold-btn {
    margin-left: 64px;
  }

  :deep(.el-scrollbar) {
    margin-bottom: 48px;
  }
}

.el-container:not(.aside-container) {
  transition: all 0.3s;
}

.header {
  transition: all 0.3s;
}

.aside-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 48px;
  background-color: #001529;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.collapse-trigger {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.65);
  transition: all 0.3s;
}

.collapse-trigger:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.08);
}

:deep(.el-scrollbar) {
  flex: 1;
  margin-bottom: 48px;
}

.el-icon--right.is-reverse {
  transform: rotate(180deg);
}

.el-icon--right {
  transition: transform 0.3s;
}
</style>
