
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { DataAnalysis, FolderOpened, Box, Warning, Tickets, Van, User, ArrowDown, FirstAidKit, House } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const active = computed(() => route.path)
const menus = [
  { path:'/admin/dashboard', label:'控制中心', icon:DataAnalysis }, { path:'/admin/categories',label:'药品分类',icon:FolderOpened },
  { path:'/admin/medicines',label:'药品管理',icon:Box }, { path:'/admin/inventory',label:'库存预警',icon:Warning },
  { path:'/admin/orders',label:'订单管理',icon:Tickets }, { path:'/admin/riders',label:'骑手管理',icon:Van }, { path:'/admin/users',label:'用户管理',icon:User }
]
const logout = async () => { await ElMessageBox.confirm('确认退出管理员账号吗？','退出登录',{type:'warning'}); await userStore.logout(); router.push('/login') }
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <div class="admin-brand"><span class="brand-mark"><FirstAidKit /></span><div><b>速安药房</b><small>运营控制中心</small></div></div>
      <el-menu class="admin-menu" :default-active="active" router background-color="transparent" text-color="#c8ddf4" active-text-color="#fff">
        <el-menu-item v-for="menu in menus" :key="menu.path" :index="menu.path"><el-icon><component :is="menu.icon" /></el-icon><span>{{ menu.label }}</span></el-menu-item>
      </el-menu>
      <div class="aside-tip"><span class="pulse-dot"></span>系统运行正常</div>
    </aside>
    <section class="admin-main">
      <header class="admin-header"><div class="crumb"><el-icon @click="router.push('/home')"><House /></el-icon><span>药店管理</span><i>/</i><b>{{ route.meta.title || '运营控制中心' }}</b></div><div class="admin-user"><span class="date-pill">{{ new Date().toLocaleDateString('zh-CN') }}</span><el-dropdown><span class="user-entry"><el-avatar :size="30">{{ userStore.user?.nickname?.slice(0,1) || '管' }}</el-avatar>{{ userStore.user?.nickname || '管理员' }}<el-icon><ArrowDown /></el-icon></span><template #dropdown><el-dropdown-menu><el-dropdown-item @click="router.push('/admin/profile')">个人信息</el-dropdown-item><el-dropdown-item @click="router.push('/home')">前台商城</el-dropdown-item><el-dropdown-item divided @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></header>
      <div class="admin-content"><router-view /></div>
    </section>
  </div>
</template>
