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
  admin_id: string
}

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await post<Result<LoginVO>>('/admin/auth/login', {
      username: username.value,
      password: password.value,
    })
    if (res.code === '00000') {
      auth.login(res.data.access_token, res.data.nickname, res.data.role, res.data.admin_id)
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
            v-model="username"
            placeholder="用户名"
            :prefix-icon="'User'"
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
  background: #f5f7fa;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.login-desc {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
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
}
</style>
