import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'echocampus_token'
const USERNAME_KEY = 'echocampus_username'
const ROLE_KEY = 'echocampus_role'
const ADMIN_ID_KEY = 'echocampus_admin_id'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const username = ref<string | null>(localStorage.getItem(USERNAME_KEY))
  const role = ref<string | null>(localStorage.getItem(ROLE_KEY))
  const adminId = ref<string | null>(localStorage.getItem(ADMIN_ID_KEY))

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  function login(accessToken: string, nickname: string, userRole: string, id: string) {
    token.value = accessToken
    username.value = nickname
    role.value = userRole
    adminId.value = id

    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USERNAME_KEY, nickname)
    localStorage.setItem(ROLE_KEY, userRole)
    localStorage.setItem(ADMIN_ID_KEY, id)
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null
    adminId.value = null

    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLE_KEY)
    localStorage.removeItem(ADMIN_ID_KEY)
  }

  return { token, username, role, adminId, isLoggedIn, isSuperAdmin, login, logout }
})
