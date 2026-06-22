<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { post, type Result } from '../api/client'

interface LoginVO {
  access_token: string
  token_type: string
  expires_in: number
  nickname: string
  role: string
  email: string
  admin_id: string
}

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await post<Result<LoginVO>>('/admin/auth/login', {
      email: email.value,
      password: password.value,
    })
    if (res.code === '00000') {
      auth.login(res.data.access_token, res.data.nickname, res.data.role, res.data.email, res.data.admin_id)
      router.replace('/landmark')
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch {
    errorMsg.value = '登录失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">映像校园后台</h1>
        <p class="login-desc">管理员登录</p>
      </div>

      <el-form class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="email"
            placeholder="邮箱"
            :prefix-icon="'Message'"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="密码"
            :prefix-icon="'Lock'"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <p v-if="errorMsg" class="login-error">{{ errorMsg }}</p>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f7f8fa;
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -180px;
  right: -120px;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(5, 150, 105, 0.04) 0%, transparent 70%);
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  bottom: -140px;
  left: -100px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(5, 150, 105, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

.login-card {
  width: 400px;
  padding: 44px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.login-desc {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
  font-weight: 500;
}

.login-form {
  display: flex;
  flex-direction: column;
}

.login-error {
  color: #ef4444;
  font-size: 13px;
  margin: -8px 0 16px 0;
  text-align: center;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
