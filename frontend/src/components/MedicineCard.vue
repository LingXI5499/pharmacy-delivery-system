
<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import type { Medicine } from '@/types'
import { money } from '@/utils'

const props = defineProps<{ medicine: Medicine }>()
const router = useRouter(); const userStore = useUserStore(); const cartStore=useCartStore(); const loading=ref(false)
const detail=()=>router.push(`/medicine/${props.medicine.id}`)
const add=async()=>{
  if (!userStore.user) return router.push({name:'login',query:{redirect:router.currentRoute.value.fullPath}})
  if (props.medicine.stock<=0 || props.medicine.status!==1) return ElMessage.warning('该商品暂时缺货')
  loading.value=true
  try { await cartApi.add({medicineId:props.medicine.id,quantity:1}); await cartStore.load(); ElMessage.success('已加入购物车') } finally { loading.value=false }
}
</script>
<template>
  <article class="medicine-card" @click="detail">
    <div class="medicine-image-wrap"><img v-if="medicine.imageUrl" :src="medicine.imageUrl" :alt="medicine.medicineName" /><span v-else class="image-fallback">药</span><span class="category-chip">{{ medicine.categoryName }}</span><span v-if="medicine.stock<=0" class="sold-mask">暂时缺货</span></div>
    <div class="medicine-info"><h3>{{ medicine.medicineName }}</h3><p>{{ medicine.description || '家庭常备商品，快速送达' }}</p><div class="stock-line" :class="{ low: medicine.isLowStock, out: medicine.stock<=0 }">{{ medicine.stock<=0 ? '暂时缺货' : medicine.isLowStock ? `库存仅剩 ${medicine.stock} 件` : `库存 ${medicine.stock} 件` }}</div><div class="card-bottom"><strong>{{ money(medicine.price) }}</strong><el-button circle type="primary" :loading="loading" :disabled="medicine.stock<=0 || medicine.status!==1" @click.stop="add"><el-icon v-if="!loading"><Plus /></el-icon></el-button></div></div>
  </article>
</template>
