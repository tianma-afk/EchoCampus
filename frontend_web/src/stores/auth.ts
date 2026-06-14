import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'echocampus_token'
const USERNAME_KEY = 'echocampus_username'
const ROLE_KEY = 'echocampus_role'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const username = ref<string | null>(localStorage.getItem(USERNAME_KEY))
  const role = ref<string | null>(localStorage.getItem(ROLE_KEY))

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  function login(accessToken: string, nickname: string, userRole: string) {
    token.value = accessToken
    username.value = nickname
    role.value = userRole

    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USERNAME_KEY, nickname)
    localStorage.setItem(ROLE_KEY, userRole)
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null

    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLE_KEY)
  }

  return { token, username, role, isLoggedIn, isSuperAdmin, login, logout }
})
