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
      router.replace('/home')
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
    <!-- Left: Brand -->
    <div class="login-brand">
      <div class="brand-content">
        <p class="brand-title">映像校园</p>
        <p class="brand-subtitle">每一帧映像，都是校园的温度</p>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="login-form-area">
      <div class="form-wrapper">
        <h2 class="form-title">欢迎登录</h2>

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
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 40%, #a7f3d0 100%);
  position: relative;
  overflow: hidden;
}

/* decorative circles */
.login-page::before {
  content: '';
  position: absolute;
  top: -120px;
  right: -80px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: rgba(5, 150, 105, 0.06);
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  bottom: -100px;
  left: -60px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(5, 150, 105, 0.08);
  pointer-events: none;
}

/* ═══════════ Left: Brand ═══════════ */
.login-brand {
  flex: 0 0 44%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.brand-content {
  text-align: left;
  padding: 40px;
}

.brand-title {
  font-size: 84px;
  font-weight: 500;
  color: #059669;
  margin: 0 0 16px 0;
  letter-spacing: 0.04em;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 36px;
  color: #34d399;
  margin: 0;
  font-weight: 400;
  letter-spacing: 0.08em;
  line-height: 1.6;
}

/* ═══════════ Right: Form ═══════════ */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.form-wrapper {
  width: 360px;
  max-width: 90%;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 36px 0;
  letter-spacing: -0.01em;
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

/* ═══════════ Responsive ═══════════ */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    flex: 0 0 auto;
    padding: 56px 32px;
  }

  .brand-title {
    font-size: 36px;
  }

  .brand-subtitle {
    font-size: 15px;
  }

  .login-form-area {
    flex: 1;
    padding: 40px 24px 60px;
  }

  .form-title {
    font-size: 20px;
    margin-bottom: 28px;
  }
}
</style>
