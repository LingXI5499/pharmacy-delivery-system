import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { guest: true } },
    {
      path: '/', component: () => import('@/layouts/UserLayout.vue'), children: [
        { path: '', redirect: '/home' },
        { path: 'home', name: 'home', component: () => import('@/views/front/HomeView.vue') },
        { path: 'category/:id?', name: 'category', component: () => import('@/views/front/MedicineListView.vue') },
        { path: 'medicine/:id', name: 'medicine-detail', component: () => import('@/views/front/MedicineDetailView.vue') },
        { path: 'cart', name: 'cart', component: () => import('@/views/front/CartView.vue'), meta: { requiresAuth: true } },
        { path: 'checkout', name: 'checkout', component: () => import('@/views/front/CheckoutView.vue'), meta: { requiresAuth: true } },
        { path: 'address', name: 'address', component: () => import('@/views/front/AddressView.vue'), meta: { requiresAuth: true } },
        { path: 'orders', name: 'orders', component: () => import('@/views/front/OrdersView.vue'), meta: { requiresAuth: true } },
        { path: 'orders/:id', name: 'order-detail', component: () => import('@/views/front/OrderDetailView.vue'), meta: { requiresAuth: true } },
        { path: 'profile', name: 'profile', component: () => import('@/views/front/ProfileView.vue'), meta: { requiresAuth: true } }
      ]
    },
    { path:'/pharmacist',name:'pharmacist-workbench',component:()=>import('@/views/workbench/PharmacistView.vue'),meta:{requiresAuth:true,roles:['PHARMACIST','ADMIN']} },
    { path:'/purchaser',name:'purchaser-workbench',component:()=>import('@/views/workbench/PurchaserView.vue'),meta:{requiresAuth:true,roles:['PURCHASER','ADMIN']} },
    { path:'/warehouse',name:'warehouse-workbench',component:()=>import('@/views/workbench/WarehouseView.vue'),meta:{requiresAuth:true,roles:['WAREHOUSE','ADMIN']} },
    {
      path: '/admin', component: () => import('@/layouts/AdminLayout.vue'), meta: { requiresAuth: true, admin: true }, children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/CategoriesView.vue') },
        { path: 'medicines', name: 'admin-medicines', component: () => import('@/views/admin/MedicinesView.vue') },
        { path: 'inventory', name: 'admin-inventory', component: () => import('@/views/admin/InventoryView.vue') },
        { path: 'orders', name: 'admin-orders', component: () => import('@/views/admin/OrdersView.vue') },
        { path: 'orders/:id', name: 'admin-order-detail', component: () => import('@/views/admin/OrderDetailView.vue') },
        { path: 'riders', name: 'admin-riders', component: () => import('@/views/admin/RidersView.vue') },
        { path: 'purchase-orders', name: 'admin-purchase-orders', component: () => import('@/views/admin/PurchaseOrdersView.vue'), meta: { title: '采购审批' } },
        { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UsersView.vue') },
        { path: 'profile', name: 'admin-profile', component: () => import('@/views/admin/ProfileView.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/home' }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  const needAuth = to.matched.some(item => item.meta.requiresAuth)
  const needAdmin = to.matched.some(item => item.meta.admin)
  const allowedRoles = to.matched.flatMap(item => (item.meta.roles as string[]|undefined) || [])
  if ((needAuth || to.meta.guest) && !store.initialized) await store.loadMe()
  if (needAuth && !store.user) return { name: 'login', query: { redirect: to.fullPath } }
  if (needAdmin && store.user?.role !== 'ADMIN') {
    ElMessage.warning('该页面仅药品管理员可访问')
    return '/home'
  }
  if (allowedRoles.length && store.user && !allowedRoles.includes(store.user.role)) { ElMessage.warning('当前角色无权访问该工作台'); return '/home' }
  if (to.meta.guest && store.user) return store.user.role === 'ADMIN' ? '/admin/dashboard' : '/home'
  return true
})

export default router
