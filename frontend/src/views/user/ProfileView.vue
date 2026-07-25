<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import JSEncrypt from 'jsencrypt'
import { changePassword, getProfile, updateProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelInput from '@/components/ui/PixelInput.vue'
import PixelToast from '@/components/ui/PixelToast'
import fallbackAvatar from '@/assets/pixel/products/avatar-placeholder.png'

const router = useRouter()
const user = useUserStore()
const loading = ref(true)
const saving = ref(false)
const changing = ref(false)
const profile = reactive({ email: '', name: '', mobile: '', avatar: '' })
const passwords = reactive({ current: '', next: '', confirm: '' })
const avatarSource = computed(() => profile.avatar || fallbackAvatar)
const mobileError = computed(() => profile.mobile && !/^1[3-9]\d{9}$/.test(profile.mobile) ? '手机号格式不正确' : '')
const passwordError = computed(() => passwords.next && [...passwords.next].length < 10 ? '新密码至少 10 个字符' : '')
const confirmError = computed(() => passwords.confirm && passwords.confirm !== passwords.next ? '两次密码不一致' : '')

function encrypted(value) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(import.meta.env.VITE_PUBLIC_KEY)
  return encryptor.encrypt(value)
}

async function load() {
  loading.value = true
  try {
    Object.assign(profile, await getProfile())
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!profile.name.trim() || mobileError.value || saving.value) return
  saving.value = true
  try {
    Object.assign(profile, await updateProfile({
      name: profile.name,
      mobile: profile.mobile,
      avatar: profile.avatar
    }))
    PixelToast.success('个人资料已保存')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!passwords.current || !passwords.next || passwordError.value
    || passwords.next !== passwords.confirm || changing.value) return
  const currentPassword = encrypted(passwords.current)
  const newPassword = encrypted(passwords.next)
  if (!currentPassword || !newPassword) return PixelToast.error('密码加密失败，请检查公钥配置')
  changing.value = true
  try {
    await changePassword({ currentPassword, newPassword })
    user.clearUserInfo()
    PixelToast.success('密码已修改，请重新登录')
    await router.replace('/login')
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="profile" v-loading="loading">
    <header><small>PROFILE</small><h1>个人资料</h1></header>
    <form class="panel profile-card" @submit.prevent="saveProfile">
      <img :src="avatarSource" alt="用户头像" data-pixel @error="$event.target.src = fallbackAvatar">
      <label>登录邮箱<PixelInput :model-value="profile.email" disabled /></label>
      <label>昵称<PixelInput v-model="profile.name" maxlength="255" /></label>
      <label>手机号<PixelInput v-model="profile.mobile" maxlength="20" placeholder="选填" :error="mobileError" /></label>
      <label>头像地址<PixelInput v-model="profile.avatar" maxlength="512" placeholder="选填，填写图片 URL" /></label>
      <PixelButton native-type="submit" :loading="saving" :disabled="!profile.name.trim() || !!mobileError">保存资料</PixelButton>
    </form>
    <form class="panel password-card" @submit.prevent="savePassword">
      <h2>账号安全</h2>
      <label>当前密码<PixelInput v-model="passwords.current" type="password" /></label>
      <label>新密码<PixelInput v-model="passwords.next" type="password" maxlength="128" :error="passwordError" /></label>
      <label>确认新密码<PixelInput v-model="passwords.confirm" type="password" maxlength="128" :error="confirmError" /></label>
      <PixelButton type="wood" native-type="submit" :loading="changing" :disabled="!passwords.current || !passwords.next || passwords.next !== passwords.confirm || !!passwordError">修改密码</PixelButton>
    </form>
  </div>
</template>

<style scoped>
.profile { display: grid; gap: var(--sp-4); }
header small { color: var(--c-moss-dark); }
h1, h2 { font-family: var(--font-pixel); color: var(--c-wood-dark); }
h1 { font-size: 30px; }
.panel { display: grid; gap: 14px; padding: var(--sp-4); border: 3px solid var(--c-wood-dark); background: var(--c-surface); box-shadow: var(--shadow-pixel); }
.profile-card { grid-template-columns: 120px minmax(0, 1fr); align-items: end; }
.profile-card img { grid-row: 1 / 5; width: 104px; height: 104px; object-fit: contain; image-rendering: pixelated; border: 2px solid var(--c-wood); background: var(--c-surface-2); }
.profile-card button, .password-card button { justify-self: start; }
.password-card { grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: end; }
.password-card h2 { grid-column: 1 / -1; }
@media (max-width: 720px) {
  .profile-card, .password-card { grid-template-columns: 1fr; }
  .profile-card img, .password-card h2 { grid-column: auto; grid-row: auto; }
}
</style>
