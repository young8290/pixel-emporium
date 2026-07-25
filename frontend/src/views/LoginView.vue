<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import JSEncrypt from 'jsencrypt'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import AuthScene from '@/components/business/AuthScene.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelInput from '@/components/ui/PixelInput.vue'
import PixelToast from '@/components/ui/PixelToast'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const touched = ref(false)
const form = reactive({ email: '', password: '' })
const emailError = computed(() => touched.value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) ? '请输入正确的邮箱' : '')
const passwordError = computed(() => touched.value && form.password.length < 4 ? '密码至少 4 位' : '')

function encrypted(password) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(import.meta.env.VITE_PUBLIC_KEY)
  return encryptor.encrypt(password)
}

async function submit() {
  touched.value = true
  if (emailError.value || passwordError.value || loading.value) return
  const cipher = encrypted(form.password)
  if (!cipher) return PixelToast.error('密码加密失败，请检查公钥配置')
  loading.value = true
  try {
    const data = await login(form.email, cipher)
    user.setUserInfo(data)
    PixelToast.success('欢迎回来')
    await router.replace(String(route.query.redirect || '/'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <AuthScene class="auth-page__scene" />
    <section class="auth-card auth-card--plaque">
      <small>WELCOME BACK</small>
      <h1>欢迎回来</h1>
      <p>回到小店，继续管理购物篮和订单。</p>
      <form @submit.prevent="submit">
        <label>邮箱<PixelInput v-model="form.email" type="email" placeholder="name@example.com" :error="emailError" /></label>
        <label>密码<PixelInput v-model="form.password" type="password" placeholder="至少 4 位" :error="passwordError" /></label>
        <PixelButton class="auth-submit auth-submit--wood" type="wood" native-type="submit" :loading="loading" block>登录</PixelButton>
      </form>
      <p class="switch">还没有账号？<RouterLink to="/register">注册一块小店招牌</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, .95fr);
  height: 100vh;
  overflow: hidden;
}
.auth-page__scene { min-height: 100vh; }
.auth-card {
  display: grid;
  align-self: center;
  gap: 12px;
  margin: clamp(24px, 5vw, 72px);
  border-style: solid;
  border-width: 32px;
  border-image: url('@/assets/pixel/auth/auth-plaque-9slice.png') 32 fill repeat;
  background: var(--c-surface);
  color: var(--c-ink);
  image-rendering: pixelated;
  padding: 8px 12px 16px;
  animation: auth-card-enter var(--dur-3) var(--ease-out) both;
}
.auth-card small { color: var(--c-moss-dark); letter-spacing: .12em; }
.auth-card h1 { color: var(--c-wood-dark); font-family: var(--font-pixel); font-size: 36px; line-height: 1.2; }
.auth-card > p { color: var(--c-ink-2); }
form { display: grid; gap: 16px; margin-top: 14px; }
label { display: grid; gap: 7px; }
.auth-submit--wood {
  --btn-bg: var(--c-moss);
  --btn-border: var(--c-wood-dark);
  border: 3px solid var(--c-wood-dark);
  border-image: none;
  background: var(--c-moss);
  color: var(--c-surface);
  letter-spacing: .12em;
  text-shadow: 1px 1px 0 color-mix(in srgb, var(--c-wood-dark) 82%, transparent);
  box-shadow: 0 3px 0 var(--c-wood-dark), inset 0 2px 0 color-mix(in srgb, var(--c-surface) 22%, transparent), inset 0 -2px 0 var(--c-moss-dark);
}
.auth-submit--wood:not(.is-disabled):not(.is-loading):hover {
  --btn-bg: var(--c-moss-dark);
  background: var(--c-moss-dark);
  box-shadow: 0 4px 0 var(--c-wood-dark), inset 0 2px 0 color-mix(in srgb, var(--c-surface) 16%, transparent), inset 0 -2px 0 color-mix(in srgb, var(--c-wood-dark) 60%, var(--c-moss-dark));
}
.switch { text-align: center; }
@keyframes auth-card-enter {
  from { opacity: 0; transform: scale(.98); }
  to { opacity: 1; transform: scale(1); }
}
@media (max-width: 760px) {
  .auth-page { grid-template-columns: 1fr; height: auto; min-height: 100vh; overflow: visible; }
  .auth-page__scene { min-height: 220px; height: 220px; }
  .auth-card { margin: 20px 12px 28px; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-card { animation: none; }
}
</style>
