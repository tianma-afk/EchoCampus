<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

const emit = defineEmits<{ back: []; logout: [] }>()

const showPasswordModal = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordError = ref('')
const passwordLoading = ref(false)

function openPasswordModal() {
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  passwordError.value = ''
  showPasswordModal.value = true
}

function closePasswordModal() {
  showPasswordModal.value = false
  passwordError.value = ''
}

async function handleChangePassword() {
  passwordError.value = ''
  if (!oldPassword.value) { passwordError.value = '请输入当前密码'; return }
  if (!newPassword.value) { passwordError.value = '请输入新密码'; return }
  if (newPassword.value.length < 6) { passwordError.value = '新密码至少6位'; return }
  if (newPassword.value !== confirmPassword.value) { passwordError.value = '两次密码不一致'; return }

  passwordLoading.value = true
  try {
    const token = localStorage.getItem('auth_token') || ''
    await axios.put('http://localhost:8080/api/v1/user/password', {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    }, { headers: { Authorization: `Bearer ${token}` } })
    showPasswordModal.value = false
  } catch (e: any) {
    passwordError.value = e?.response?.data?.message || '修改失败，请重试'
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="settings-page">
    <div class="settings-header">
      <button class="settings-back" @click="emit('back')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <span class="settings-title">通用设置</span>
      <div style="width: 40px" />
    </div>

    <div class="settings-body">
      <div class="section">
        <h3 class="section-title">账号安全</h3>
        <div class="menu-card">
          <div class="menu-item" @click="openPasswordModal">
            <div class="menu-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0110 0v4" />
              </svg>
            </div>
            <span class="menu-label">修改密码</span>
            <svg class="menu-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </div>
      </div>

      <button class="logout-btn" @click="emit('logout')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" y1="12" x2="9" y2="12" />
        </svg>
        退出登录
      </button>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click.self="closePasswordModal">
      <div class="modal-card">
        <h3 class="modal-title">修改密码</h3>
        <div class="modal-fields">
          <div class="modal-field">
            <label class="modal-label">当前密码</label>
            <input
              v-model="oldPassword"
              type="password"
              class="modal-input"
              placeholder="请输入当前密码"
              :disabled="passwordLoading"
            />
          </div>
          <div class="modal-field">
            <label class="modal-label">新密码</label>
            <input
              v-model="newPassword"
              type="password"
              class="modal-input"
              placeholder="请输入新密码（至少6位）"
              :disabled="passwordLoading"
            />
          </div>
          <div class="modal-field">
            <label class="modal-label">确认新密码</label>
            <input
              v-model="confirmPassword"
              type="password"
              class="modal-input"
              placeholder="请再次输入新密码"
              :disabled="passwordLoading"
            />
          </div>
        </div>
        <p v-if="passwordError" class="modal-error">{{ passwordError }}</p>
        <div class="modal-actions">
          <button class="modal-btn cancel" :disabled="passwordLoading" @click="closePasswordModal">取消</button>
          <button class="modal-btn confirm" :disabled="passwordLoading" @click="handleChangePassword">
            {{ passwordLoading ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  flex-shrink: 0;
}

.settings-back {
  width: 40px;
  height: 40px;
  border: none;
  background: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.settings-back svg {
  width: 24px;
  height: 24px;
  color: var(--color-text-heading);
}

.settings-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
}

.settings-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.settings-body::-webkit-scrollbar { display: none; }

.section { display: flex; flex-direction: column; gap: 10px; }

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0;
  padding-left: 4px;
}

.menu-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
}

.menu-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-icon svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
}

.menu-label {
  flex: 1;
  font-size: 14px;
  color: var(--color-text-heading);
}

.menu-arrow {
  width: 16px;
  height: 16px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-xl);
  background: var(--color-bg-card);
  color: var(--color-danger);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.logout-btn svg {
  width: 18px;
  height: 18px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.modal-card {
  width: 100%;
  max-width: 340px;
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-heading);
  margin: 0;
}

.modal-fields {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding-left: 4px;
}

.modal-input {
  width: 100%;
  height: 44px;
  border: 1px solid var(--color-divider);
  border-radius: 10px;
  padding: 0 14px;
  font-size: 15px;
  color: var(--color-text-heading);
  outline: none;
  box-sizing: border-box;
  background: #fff;
}

.modal-input:focus { border-color: var(--color-primary); }

.modal-error {
  font-size: 13px;
  color: var(--color-danger);
  margin: 0;
  text-align: center;
}

.modal-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.modal-btn {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.modal-btn.cancel {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}

.modal-btn.confirm {
  background: var(--color-primary);
  color: #fff;
}

.modal-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
