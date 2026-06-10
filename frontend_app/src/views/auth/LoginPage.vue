<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '../../composables/useAuth'

const emit = defineEmits<{ 'login-success': [] }>()

const { sendCode, login, loginWithPassword } = useAuth()

const isRegister = ref(false)
const email = ref('')
const code = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)
const errorMsg = ref('')
const codeCountdown = ref(0)
const codeSent = ref(false)

let countdownTimer: ReturnType<typeof setInterval> | null = null

function toggleMode() {
  isRegister.value = !isRegister.value
  errorMsg.value = ''
  code.value = ''
  password.value = ''
  nickname.value = ''
}

async function handleSendCode() {
  if (!email.value) {
    errorMsg.value = '请输入邮箱'
    return
  }
  errorMsg.value = ''
  try {
    await sendCode(email.value)
    codeSent.value = true
    codeCountdown.value = 60
    countdownTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    errorMsg.value = err?.response?.data?.message || '发送失败'
  }
}

async function handleLogin() {
  if (!email.value || !password.value) {
    errorMsg.value = '请填写邮箱和密码'
    return
  }
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await loginWithPassword(email.value, password.value)
    if (res.code === '00000') {
      emit('login-success')
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    errorMsg.value = err?.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!email.value || !code.value) {
    errorMsg.value = '请填写邮箱和验证码'
    return
  }
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await login({
      email: email.value,
      code: code.value,
      password: password.value,
      nickname: nickname.value
    })
    if (res.code === '00000') {
      emit('login-success')
    } else {
      errorMsg.value = res.message || '注册失败'
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    errorMsg.value = err?.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
          <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5" />
        </svg>
      </div>
      <h1 class="title">EchoCampus</h1>
      <p class="subtitle">{{ isRegister ? '创建账号以开始使用' : '欢迎回来' }}</p>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <!-- 登录模式 -->
      <div v-if="!isRegister" class="form">
        <div class="input-row">
          <input
            v-model="email"
            type="email"
            placeholder="邮箱"
            class="input"
            autocomplete="email"
          />
        </div>
        <div class="input-row">
          <input
            v-model="password"
            type="password"
            placeholder="密码"
            class="input"
            autocomplete="current-password"
          />
        </div>
        <button class="submit-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </div>

      <!-- 注册模式 -->
      <div v-else class="form">
        <div class="input-row">
          <input
            v-model="email"
            type="email"
            placeholder="邮箱"
            class="input"
            autocomplete="email"
          />
        </div>
        <div class="code-row">
          <input
            v-model="code"
            type="text"
            placeholder="验证码"
            class="input code-input"
            maxlength="6"
            autocomplete="one-time-code"
          />
          <button
            class="send-code-btn"
            :disabled="codeCountdown > 0 || !email"
            @click="handleSendCode"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s` : codeSent ? '重新发送' : '获取验证码' }}
          </button>
        </div>
        <div class="input-row">
          <input
            v-model="nickname"
            type="text"
            placeholder="昵称"
            class="input"
            maxlength="20"
            autocomplete="nickname"
          />
        </div>
        <div class="input-row">
          <input
            v-model="password"
            type="password"
            placeholder="设置密码（6-20位）"
            class="input"
            autocomplete="new-password"
          />
        </div>
        <button class="submit-btn" :disabled="loading" @click="handleRegister">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </div>

      <div class="toggle-row">
        <span v-if="!isRegister">还没有账号？</span>
        <span v-else>已有账号？</span>
        <button class="toggle-btn" @click="toggleMode">
          {{ isRegister ? '去登录' : '去注册' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f5e9 0%, #f0f7f4 50%, #e0f0e8 100%);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: #fff;
  border-radius: 24px;
  padding: 40px 28px 24px;
  box-shadow: 0 8px 32px rgba(45, 138, 110, 0.12);
}

.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  background: #2d8a6e;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo svg {
  width: 30px;
  height: 30px;
  color: #fff;
}

.title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.subtitle {
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  margin: 0 0 24px;
}

.error-msg {
  background: #fef2f2;
  color: #ef4444;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 10px;
  margin-bottom: 16px;
  text-align: center;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-row {
  width: 100%;
}

.input {
  width: 100%;
  height: 48px;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 14px;
  font-size: 15px;
  color: #1a1a1a;
  background: #fafafa;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.input:focus {
  border-color: #2d8a6e;
  background: #fff;
}

.input::placeholder {
  color: #c5c9cc;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  flex-shrink: 0;
  height: 48px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: #e8f5e9;
  color: #2d8a6e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.send-code-btn:disabled {
  background: #f3f4f6;
  color: #c5c9cc;
  cursor: not-allowed;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  background: #2d8a6e;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 4px;
  transition: background 0.2s;
}

.submit-btn:disabled {
  background: #a5d6c5;
  cursor: not-allowed;
}

.submit-btn:active:not(:disabled) {
  background: #237a5e;
}

.toggle-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 20px;
  font-size: 13px;
  color: #9ca3af;
}

.toggle-btn {
  border: none;
  background: none;
  color: #2d8a6e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}
</style>
