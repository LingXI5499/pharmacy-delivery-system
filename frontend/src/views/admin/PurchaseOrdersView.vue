<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { procurementApi } from '@/api'

const rows = ref<any[]>([])
const detail = ref<any>()
const statusFilter = ref('DRAFT')

const statusLabel = (status: string) => ({
  DRAFT: '待审批',
  APPROVED: '已批准',
  PARTIALLY_RECEIVED: '部分收货',
  RECEIVED: '已完成收货',
  REJECTED: '已拒绝'
}[status] || status)

const load = async () => {
  rows.value = await procurementApi.adminOrders(statusFilter.value ? { status: statusFilter.value } : {})
}

const openDetail = async (row: any) => {
  detail.value = await procurementApi.adminDetail(row.id)
}

const approve = async (row: any) => {
  await ElMessageBox.confirm(`确认批准采购单 ${row.purchaseNo}？`, '审批通过', { type: 'warning' })
  await procurementApi.approve(row.id)
  ElMessage.success('采购单已批准')
  await load()
  if (detail.value?.id === row.id) detail.value = await procurementApi.adminDetail(row.id)
}

const reject = async (row: any) => {
  const { value } = await ElMessageBox.prompt('请填写拒绝原因', '拒绝采购单', {
    inputValidator: (v: string) => !!v?.trim() || '拒绝原因不能为空'
  })
  await procurementApi.reject(row.id, value.trim())
  ElMessage.success('采购单已拒绝')
  await load()
  if (detail.value?.id === row.id) detail.value = await procurementApi.adminDetail(row.id)
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h1>采购审批</h1>
        <p>审批或拒绝草稿采购单；拒绝必须填写原因。</p>
      </div>
      <el-select v-model="statusFilter" style="width: 180px" @change="load">
        <el-option label="待审批" value="DRAFT" />
        <el-option label="已批准" value="APPROVED" />
        <el-option label="部分收货" value="PARTIALLY_RECEIVED" />
        <el-option label="已完成" value="RECEIVED" />
        <el-option label="已拒绝" value="REJECTED" />
        <el-option label="全部" value="" />
      </el-select>
    </div>

    <section class="control-card">
      <el-table :data="rows">
        <el-table-column prop="purchaseNo" label="单号" />
        <el-table-column prop="supplierName" label="供应商" />
        <el-table-column label="状态">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="approve(row)">批准</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="reject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="detail" class="control-card">
      <h2>{{ detail.purchaseNo }} · {{ statusLabel(detail.status) }}</h2>
      <p>供应商：{{ detail.supplierName }}</p>
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
.admin-page-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:18px}
.admin-page-head h1{margin:0 0 6px;font-size:24px}
.admin-page-head p{margin:0;color:#718198}
.control-card{background:#fff;border:1px solid #e6edf7;border-radius:14px;padding:20px;margin-bottom:16px}
.control-card h2{margin:0 0 12px;font-size:18px}
.reject{color:#c45656}
</style>
