import { ref, computed } from 'vue'
import { sendCode as apiSendCode, register as apiRegister, login as apiLogin, getProfile as apiGetProfile, type LoginResult } from '../api/auth'

const TOKEN_KEY = 'auth_token'
const NICKNAME_KEY = 'auth_nickname'
const EMAIL_KEY = 'auth_email'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const nickname = ref(localStorage.getItem(NICKNAME_KEY) || '')
const email = ref(localStorage.getItem(EMAIL_KEY) || '')
const loading = ref(true)

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  async function tryRestoreSession() {
    const savedToken = localStorage.getItem(TOKEN_KEY)
    if (!savedToken) {
      loading.value = false
      return
    }
    try {
      const res = await apiGetProfile(savedToken)
      if (res.code === '00000') {
        token.value = savedToken
        nickname.value = res.data.nickname
        email.value = res.data.email
        localStorage.setItem(NICKNAME_KEY, res.data.nickname)
        localStorage.setItem(EMAIL_KEY, res.data.email)
      } else {
        clearAuth()
      }
    } catch {
      clearAuth()
    } finally {
      loading.value = false
    }
  }

  async function sendCode(toEmail: string) {
    return apiSendCode(toEmail)
  }

  async function login(params: { email: string; code: string; password: string; nickname: string }) {
    const res = await apiRegister(params)
    if (res.code === '00000') {
      saveAuth(res.data, params.email)
    }
    return res
  }

  function clearAuth() {
    token.value = ''
    nickname.value = ''
    email.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NICKNAME_KEY)
    localStorage.removeItem(EMAIL_KEY)
  }

  function logout() {
    clearAuth()
  }

  function saveAuth(data: LoginResult, userEmail?: string) {
    token.value = data.access_token
    nickname.value = data.nickname
    if (userEmail) {
      email.value = userEmail
      localStorage.setItem(EMAIL_KEY, userEmail)
    }
    localStorage.setItem(TOKEN_KEY, data.access_token)
    localStorage.setItem(NICKNAME_KEY, data.nickname)
  }

  async function loginWithPassword(userEmail: string, userPassword: string) {
    const res = await apiLogin({ email: userEmail, password: userPassword })
    if (res.code === '00000') {
      saveAuth(res.data, userEmail)
    }
    return res
  }

  return { token, nickname, email, isLoggedIn, loading, tryRestoreSession, sendCode, login, loginWithPassword, logout }
}
