<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { procurementApi, publicApi } from '@/api'

const suppliers = ref<any[]>([])
const orders = ref<any[]>([])
const medicines = ref<any[]>([])
const detail = ref<any>()
const supplierForm = reactive({ supplierCode: '', supplierName: '', contactName: '', phone: '' })
const orderForm = reactive({
  supplierId: undefined as number | undefined,
  remark: '',
  items: [{ medicineId: undefined as number | undefined, quantity: 1, purchasePrice: 1 }]
})

const statusLabel = (status: string) => ({
  DRAFT: '待审批',
  APPROVED: '已批准',
  PARTIALLY_RECEIVED: '部分收货',
  RECEIVED: '已完成收货',
  REJECTED: '已拒绝'
}[status] || status)

const load = async () => {
  const [supplierRows, orderRows, medicinePage] = await Promise.all([
    procurementApi.suppliers(),
    procurementApi.purchaserOrders(),
    publicApi.medicines({ page: 1, size: 100, sort: 'default' })
  ])
  suppliers.value = supplierRows
  orders.value = orderRows
  medicines.value = medicinePage.records || []
}

const saveSupplier = async () => {
  await procurementApi.addSupplier(supplierForm)
  ElMessage.success('供应商已创建')
  Object.assign(supplierForm, { supplierCode: '', supplierName: '', contactName: '', phone: '' })
  await load()
}

const addItem = () => orderForm.items.push({ medicineId: undefined, quantity: 1, purchasePrice: 1 })
const removeItem = (index: number) => {
  if (orderForm.items.length === 1) return
  orderForm.items.splice(index, 1)
}

const createOrder = async () => {
  if (!orderForm.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }
  if (orderForm.items.some(item => !item.medicineId || item.quantity <= 0 || item.purchasePrice <= 0)) {
    ElMessage.warning('请完整填写采购明细')
    return
  }
  const created = await procurementApi.createOrder({
    supplierId: orderForm.supplierId,
    remark: orderForm.remark || undefined,
    items: orderForm.items.map(item => ({
      medicineId: item.medicineId,
      quantity: item.quantity,
      purchasePrice: item.purchasePrice
    }))
  })
  ElMessage.success(`采购单 ${created.purchaseNo} 已提交，等待管理员审批`)
  orderForm.remark = ''
  orderForm.items = [{ medicineId: undefined, quantity: 1, purchasePrice: 1 }]
  await load()
  detail.value = await procurementApi.purchaserDetail(created.id)
}

const openDetail = async (row: any) => {
  detail.value = await procurementApi.purchaserDetail(row.id)
}

onMounted(load)
</script>

<template>
  <div class="workbench">
    <h1>采购工作台</h1>
    <p>维护供应商、创建采购单，并查看审批与收货状态。前端校验不能替代后端校验。</p>

    <section class="panel">
      <h2>供应商</h2>
      <el-form :inline="true" class="form-row">
        <el-form-item label="编码"><el-input v-model="supplierForm.supplierCode" maxlength="32" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="supplierForm.supplierName" maxlength="100" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="supplierForm.contactName" maxlength="50" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="supplierForm.phone" maxlength="20" /></el-form-item>
        <el-button type="primary" @click="saveSupplier">新增供应商</el-button>
      </el-form>
      <el-table :data="suppliers">
        <el-table-column prop="supplierCode" label="编码" />
        <el-table-column prop="supplierName" label="名称" />
        <el-table-column prop="contactName" label="联系人" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </section>

    <section class="panel">
      <h2>创建采购单</h2>
      <el-form label-width="90px">
        <el-form-item label="供应商">
          <el-select v-model="orderForm.supplierId" placeholder="选择供应商" filterable style="width: 320px">
            <el-option v-for="s in suppliers" :key="s.id" :label="`${s.supplierCode} · ${s.supplierName}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="orderForm.remark" maxlength="255" style="width: 480px" /></el-form-item>
        <div v-for="(item, index) in orderForm.items" :key="index" class="item-row">
          <el-select v-model="item.medicineId" placeholder="药品" filterable style="width: 260px">
            <el-option v-for="m in medicines" :key="m.id" :label="m.medicineName" :value="m.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <el-input-number v-model="item.purchasePrice" :min="0.01" :step="0.01" :precision="2" />
          <el-button @click="addItem">加行</el-button>
          <el-button v-if="orderForm.items.length > 1" @click="removeItem(index)">删除</el-button>
        </div>
        <el-button type="primary" @click="createOrder">提交采购单</el-button>
      </el-form>
    </section>

    <section class="panel">
      <h2>我的采购单</h2>
      <el-table :data="orders" @row-click="openDetail">
        <el-table-column prop="purchaseNo" label="单号" />
        <el-table-column prop="supplierName" label="供应商" />
        <el-table-column label="状态">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }"><el-button link type="primary" @click.stop="openDetail(row)">明细</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="detail" class="panel">
      <h2>采购明细 · {{ detail.purchaseNo }}</h2>
      <p>状态：{{ statusLabel(detail.status) }}；供应商：{{ detail.supplierName }}</p>
      <p v-if="detail.rejectReason" class="reject">拒绝原因：{{ detail.rejectReason }}</p>
      <el-table :data="detail.items || []">
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="orderedQty" label="订购" />
        <el-table-column prop="receivedQty" label="已收" />
        <el-table-column prop="unreceivedQty" label="未收" />
        <el-table-column prop="purchasePrice" label="采购价" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.workbench{max-width:1100px;margin:40px auto;padding:24px;background:#fff;border-radius:16px}
.workbench>p,.panel>p{color:#718198}
.panel{margin-top:28px}
.panel h2{margin:0 0 14px;font-size:18px}
.item-row{display:flex;gap:10px;align-items:center;margin-bottom:10px;flex-wrap:wrap}
.reject{color:#c45656}
</style>
