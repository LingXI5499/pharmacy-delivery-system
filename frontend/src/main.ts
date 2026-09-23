
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/main.css'
import App from './App.vue'
import router from './router'
import { AUTH_CHANGE_KEY, useUserStore } from './stores/user'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')

const userStore = useUserStore(pinia)
let syncing = false
const syncSession = async () => {
  if (syncing) return
  syncing = true
  try {
    const user = await userStore.loadMe()
    const route = router.currentRoute.value
    const isAdminPage = route.matched.some(item => item.meta.admin)
    if (isAdminPage && user?.role !== 'ADMIN') {
      ElMessage.warning(user ? '当前浏览器已切换为普通用户，请重新登录管理员账号' : '管理员登录已失效，请重新登录')
      await router.replace({ path: '/login', query: { redirect: route.fullPath, switch: '1' } })
    } else if (!user && route.matched.some(item => item.meta.requiresAuth)) {
      ElMessage.warning('登录已失效，请重新登录')
      await router.replace({ path: '/login', query: { redirect: route.fullPath } })
    }
  } finally { syncing = false }
}

window.addEventListener('storage', event => { if (event.key === AUTH_CHANGE_KEY) void syncSession() })
window.addEventListener('focus', () => { void syncSession() })
window.addEventListener('pharmacy-auth-rejected', () => { void syncSession() })
