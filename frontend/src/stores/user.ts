
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)
  const setUser = (value: User | null) => { user.value = value }
  const loadMe = async () => {
    try { user.value = await authApi.me(); return user.value } catch { user.value = null; return null } finally { initialized.value = true }
  }
  const logout = async () => { try { await authApi.logout() } finally { user.value = null; initialized.value = true } }
  return { user, initialized, setUser, loadMe, logout }
})
