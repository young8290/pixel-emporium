<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import JSEncrypt from 'jsencrypt'
import { register, sendRegistrationCode } from '@/api/auth'
import AuthScene from '@/components/business/AuthScene.vue'
import PixelButton from '@/components/ui/PixelButton.vue'
import PixelInput from '@/components/ui/PixelInput.vue'
import PixelToast from '@/components/ui/PixelToast'

const router = useRouter()
const loading = ref(false)
const codeSending = ref(false)
const cooldown = ref(0)
let cooldownTimer = null
const touched = ref(false)
const form = reactive({ email: '', emailCode: '', password: '', confirm: '', name: '', mobile: '' })
const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
const valid = computed(() => emailValid.value && /^\d{6}$/.test(form.emailCode) && [...form.password].length >= 10 && form.password === form.confirm && form.name && (!form.mobile || /^1[3-9]\d{9}$/.test(form.mobile)))

function encrypted(password) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(import.meta.env.VITE_PUBLIC_KEY)
  return encryptor.encrypt(password)
}

async function submit() {
  touched.value = true
  if (!valid.value || loading.value) return
  const password = encrypted(form.password)
  if (!password) return PixelToast.error('密码加密失败，请检查公钥配置')
  loading.value = true
  try {
    await register({ email: form.email, emailCode: form.emailCode, password, name: form.name, mobile: form.mobile })
    PixelToast.success('注册成功，请登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}

function startCooldown(seconds = 60) {
  cooldown.value = seconds
  if (cooldownTimer) window.clearInterval(cooldownTimer)
  cooldownTimer = window.setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) { window.clearInterval(cooldownTimer); cooldownTimer = null }
  }, 1000)
}

async function sendCode() {
  if (!emailValid.value || codeSending.value || cooldown.value > 0) return
  codeSending.value = true
  try {
    const data = await sendRegistrationCode(form.email)
    startCooldown(Number(data?.cooldownSeconds) || 60)
    PixelToast.success('验证码已发送，请查收邮箱')
  } catch {
    cooldown.value = 0
  } finally {
    codeSending.value = false
  }
}

onUnmounted(() => { if (cooldownTimer) window.clearInterval(cooldownTimer) })
</script>

<template>
  <main class="auth-page">
    <AuthScene class="auth-page__scene" />
    <section class="auth-card auth-card--plaque auth-card--register">
      <small>NEW SHOPKEEPER</small>
      <h1>新开一间小店</h1>
      <form @submit.prevent="submit">
        <label>昵称<PixelInput v-model="form.name" :error="touched && !form.name ? '请填写昵称' : ''" /></label>
        <label>邮箱<PixelInput v-model="form.email" type="email" :error="touched && !emailValid ? '邮箱格式不正确' : ''" /></label>
        <label>邮箱验证码<div class="code-row"><PixelInput v-model="form.emailCode" inputmode="numeric" maxlength="6" :error="touched && !/^\d{6}$/.test(form.emailCode) ? '请输入 6 位验证码' : ''" /><PixelButton data-test="send-email-code" size="small" type="wood" native-type="button" :loading="codeSending" :disabled="!emailValid || cooldown > 0" @click="sendCode">{{ cooldown > 0 ? `${cooldown}s` : '发送验证码' }}</PixelButton></div></label>
        <label>手机号（选填）<PixelInput v-model="form.mobile" :error="touched && form.mobile && !/^1[3-9]\d{9}$/.test(form.mobile) ? '手机号格式不正确' : ''" /></label>
        <label>密码<PixelInput v-model="form.password" type="password" maxlength="128" :error="touched && [...form.password].length < 10 ? '密码至少 10 个字符' : ''" /></label>
        <label>确认密码<PixelInput v-model="form.confirm" type="password" :error="touched && form.confirm !== form.password ? '两次密码不一致' : ''" /></label>
        <PixelButton class="auth-submit auth-submit--wood" type="wood" native-type="submit" :loading="loading" block>完成注册</PixelButton>
      </form>
      <p>已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, .95fr);
  min-height: 100vh;
}
.auth-page__scene { min-height: 100vh; }
.auth-card {
  display: grid;
  align-self: center;
  gap: 12px;
  max-height: calc(100vh - 48px);
  margin: 24px clamp(24px, 5vw, 72px);
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
.auth-card--register { grid-template-rows: auto auto minmax(0, 1fr) auto; }
form { display: grid; gap: 12px; min-height: 0; overflow-y: auto; }
label { display: grid; gap: 5px; }
.code-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; align-items: start; }
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
.auth-card > p { color: var(--c-ink-2); text-align: center; }
@keyframes auth-card-enter {
  from { opacity: 0; transform: scale(.98); }
  to { opacity: 1; transform: scale(1); }
}
@media (max-width: 760px) {
  .auth-page { grid-template-columns: 1fr; }
  .auth-page__scene { min-height: 220px; height: 220px; }
  .auth-card { max-height: none; margin: 20px 12px 28px; }
  form { overflow-y: visible; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-card { animation: none; }
}
</style>
