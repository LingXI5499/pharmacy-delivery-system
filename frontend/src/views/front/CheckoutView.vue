<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addressApi, orderApi, prescriptionApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import type { Address, CartItem } from '@/types'
import { fullAddress, money } from '@/utils'

const router=useRouter(),route=useRoute(),store=useCartStore()
const addresses=ref<Address[]>([]),selectedAddressId=ref<number>(),remark=ref(''),submitting=ref(false)
const prescriptionFile=ref<File>(),prescriptionId=ref<number>()
const ids=computed(()=>String(route.query.ids||'').split(',').map(Number).filter(Boolean))
const items=computed<CartItem[]>(()=>store.cart.items.filter(i=>ids.value.includes(i.cartItemId)&&i.available))
const total=computed(()=>items.value.reduce((s,i)=>s+i.subtotalAmount,0))
const needsPrescription=computed(()=>items.value.some(i=>i.prescriptionRequired))
onMounted(async()=>{await store.load();addresses.value=await addressApi.list();selectedAddressId.value=addresses.value.find(a=>a.isDefault===1)?.id||addresses.value[0]?.id})
const choosePrescription=(e:Event)=>{prescriptionFile.value=(e.target as HTMLInputElement).files?.[0]}
const submit=async()=>{
  if(!selectedAddressId.value)return ElMessage.warning('请选择收货地址')
  if(!items.value.length)return router.push('/cart')
  if(needsPrescription.value&&!prescriptionFile.value)return ElMessage.warning('处方药订单必须上传处方')
  submitting.value=true
  try{
    if(needsPrescription.value&&!prescriptionId.value){const rx=await prescriptionApi.upload(prescriptionFile.value!,items.value.map(i=>i.medicineId),items.value.map(i=>i.quantity));prescriptionId.value=rx.id}
    const res=await orderApi.create({addressId:selectedAddressId.value,cartItemIds:items.value.map(i=>i.cartItemId),prescriptionId:prescriptionId.value,userRemark:remark.value})
    ElMessage.success(needsPrescription.value?'已提交，等待药师审核':'已预占库存，请在 30 分钟内模拟支付');await store.load();router.replace(`/orders/${res.orderId}`)
  }finally{submitting.value=false}
}
</script>

<template>
  <div class="page-container checkout-page">
    <div class="page-head"><span class="eyebrow">CHECKOUT</span><h1>确认订单</h1><p>批次库存将在提交时按 FEFO 规则预占。</p></div>
    <div class="checkout-grid">
      <main>
        <section class="panel"><h2>配送地址</h2><label v-for="a in addresses" :key="a.id" class="address"><input v-model="selectedAddressId" type="radio" :value="a.id"><span><b>{{a.receiverName}} {{a.receiverPhone}}</b><small>{{fullAddress(a)}}</small></span></label></section>
        <section class="panel"><h2>商品清单</h2><div v-for="item in items" :key="item.cartItemId" class="line"><span>{{item.medicineName}} × {{item.quantity}} <el-tag v-if="item.prescriptionRequired" type="warning" size="small">处方药</el-tag></span><b>{{money(item.subtotalAmount)}}</b></div></section>
        <section v-if="needsPrescription" class="panel"><h2>处方凭证</h2><p>只允许真实内容为 PDF、JPEG、PNG，最大 10 MB。</p><input type="file" accept="application/pdf,image/jpeg,image/png" @change="choosePrescription"><small>文件不在 Web 静态目录，仅本人和药师可鉴权查看。</small></section>
        <section class="panel"><h2>订单备注</h2><el-input v-model="remark" type="textarea" :rows="3" maxlength="255" show-word-limit/></section>
      </main>
      <aside class="panel total"><h2>费用明细</h2><div><span>商品总额</span><b>{{money(total)}}</b></div><div><span>配送费</span><b>¥5.00</b></div><strong>{{money(total+5)}}</strong><el-button type="primary" size="large" :loading="submitting" @click="submit">提交订单</el-button><p>仅使用虚构数据与模拟支付。</p></aside>
    </div>
  </div>
</template>

<style scoped>
.page-head{margin-bottom:20px}.eyebrow{color:#1677ff;font-size:11px;letter-spacing:2px}.checkout-grid{display:grid;grid-template-columns:1fr 320px;gap:20px}.panel{background:#fff;border:1px solid #e6edf7;border-radius:15px;padding:20px;margin-bottom:16px}.panel h2{font-size:16px;margin:0 0 16px}.address{display:flex;gap:10px;padding:12px;border:1px solid #edf1f6;border-radius:10px;margin-top:8px}.address span,.address small{display:block}.address small,.panel p,.panel>small{color:#7d8ca0;font-size:12px}.line,.total>div{display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid #f0f3f6}.total{height:max-content;position:sticky;top:88px}.total>strong{display:block;color:#ff6d2c;font-size:26px;text-align:right;margin:20px 0}.total .el-button{width:100%}@media(max-width:720px){.checkout-grid{grid-template-columns:1fr}}
</style>
