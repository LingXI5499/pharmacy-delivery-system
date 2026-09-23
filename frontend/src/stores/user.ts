
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'
import type { User } from '@/types'

export const AUTH_CHANGE_KEY = 'pharmacy-auth-changed'

const notifyOtherTabs = () => {
  try { localStorage.setItem(AUTH_CHANGE_KEY, String(Date.now())) } catch { /* Storage may be unavailable. */ }
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)
  const setUser = (value: User | null) => { user.value = value; initialized.value = true; notifyOtherTabs() }
  const loadMe = async () => {
    try { user.value = await authApi.me(); return user.value } catch { user.value = null; return null } finally { initialized.value = true }
  }
  const logout = async () => { try { await authApi.logout() } finally { user.value = null; initialized.value = true; notifyOtherTabs() } }
  return { user, initialized, setUser, loadMe, logout }
})
