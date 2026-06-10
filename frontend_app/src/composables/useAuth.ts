import { ref, computed } from 'vue'
import { sendCode as apiSendCode, register as apiRegister, login as apiLogin, type LoginResult } from '../api/auth'

const TOKEN_KEY = 'auth_token'
const NICKNAME_KEY = 'auth_nickname'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const nickname = ref(localStorage.getItem(NICKNAME_KEY) || '')

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  async function sendCode(toEmail: string) {
    return apiSendCode(toEmail)
  }

  async function login(params: { email: string; code: string; password: string; nickname: string }) {
    const res = await apiRegister(params)
    if (res.code === '00000') {
      saveAuth(res.data)
    }
    return res
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NICKNAME_KEY)
  }

  function saveAuth(data: LoginResult) {
    token.value = data.access_token
    nickname.value = data.nickname
    localStorage.setItem(TOKEN_KEY, data.access_token)
    localStorage.setItem(NICKNAME_KEY, data.nickname)
  }

  async function loginWithPassword(userEmail: string, userPassword: string) {
    const res = await apiLogin({ email: userEmail, password: userPassword })
    if (res.code === '00000') {
      saveAuth(res.data)
    }
    return res
  }

  return { token, nickname, isLoggedIn, sendCode, login, loginWithPassword, logout }
}
