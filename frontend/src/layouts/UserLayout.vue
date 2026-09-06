
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ShoppingCart, Tickets, Search, UserFilled, ArrowDown, FirstAidKit } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const keyword = ref('')
const user = computed(() => userStore.user)

onMounted(async () => {
  if (!userStore.initialized) await userStore.loadMe()
  if (userStore.user?.role === 'USER') { try { await cartStore.load() } catch { cartStore.clear() } }
})
const search = () => router.push({ name: 'category', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
const goCart = () => user.value ? router.push('/cart') : router.push({name:'login', query:{redirect:'/cart'}})
const logout = async () => {
  await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', { type:'warning' })
  await userStore.logout(); cartStore.clear(); router.push('/home')
}
</script>

<template>
  <div class="app-shell">
    <header class="shop-header">
      <div class="shop-header-inner">
        <div class="brand" @click="router.push('/home')">
          <span class="brand-mark"><FirstAidKit /></span>
          <span><b>速安药房</b><em>安心送达</em></span>
        </div>
        <el-input v-model="keyword" class="global-search" placeholder="搜索感冒药、维生素、体温计…" :prefix-icon="Search" clearable @keyup.enter="search">
          <template #append><el-button @click="search">搜药</el-button></template>
        </el-input>
        <nav class="header-actions">
          <el-button text @click="router.push('/orders')"><el-icon><Tickets /></el-icon>我的订单</el-button>
          <el-badge :value="cartStore.count" :hidden="!cartStore.count" class="cart-badge">
            <el-button class="cart-trigger" @click="goCart"><el-icon><ShoppingCart /></el-icon>购物车</el-button>
          </el-badge>
          <template v-if="user">
            <el-dropdown>
              <span class="user-entry"><el-avatar :size="30" class="avatar">{{ user.nickname.slice(0,1) }}</el-avatar>{{ user.nickname }}<el-icon><ArrowDown /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="user.role==='ADMIN'" @click="router.push('/admin/dashboard')">进入后台</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="router.push('/register')">注册</el-button>
          </template>
        </nav>
      </div>
    </header>
    <main class="shop-content"><router-view /></main>
    <footer class="shop-footer"><b>速安药房</b><span>课程设计演示系统 · 单店即时配送 · 非真实医疗服务</span><span>© 2026 Pharmacy Delivery</span></footer>
  </div>
</template>
