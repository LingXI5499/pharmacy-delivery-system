<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inventoryApi } from '@/api'

const tab = ref('batches')
const expiryDays = ref(30)
const batches = ref<any[]>([])
const ledger = ref<any[]>([])
const counts = ref<any[]>([])
const activeCount = ref<any | null>(null)
const countItems = ref<any[]>([])
const mismatches = ref<any[]>([])

const loadBatches = async () => {
  const params = expiryDays.value > 0 ? { expiringDays: expiryDays.value } : {}
  batches.value = await inventoryApi.batches(params)
  ledger.value = await inventoryApi.ledger()
}
const loadCounts = async () => {
  counts.value = await inventoryApi.counts()
}
const loadRecon = async () => {
  mismatches.value = await inventoryApi.reconciliation()
}
const load = async () => {
  await Promise.all([loadBatches(), loadCounts(), loadRecon()])
}
onMounted(load)

const adjust = async (row: any) => {
  const { value: type } = await ElMessageBox.prompt('INCREMENT 增加 / DECREMENT 减少 / SET 设定', '调整批次库存', {
    inputValue: 'INCREMENT',
    inputValidator: (v) => ['INCREMENT', 'DECREMENT', 'SET'].includes(v.toUpperCase()) || '请输入有效类型'
  })
  const { value: quantity } = await ElMessageBox.prompt('请输入非负整数数量', '调整批次库存', {
    inputValue: '1',
    inputValidator: (v) => /^\d+$/.test(v) || '请输入非负整数'
  })
  const { value: reason } = await ElMessageBox.prompt('必须填写可审计的调整原因', '调整批次库存', {
    inputValidator: (v) => !!v.trim() || '原因不能为空'
  })
  await inventoryApi.adjust(row.id, { adjustType: type.toUpperCase(), quantity: Number(quantity), reason })
  ElMessage.success('批次库存与库存台账已同步更新')
  await load()
}

const createCount = async () => {
  await inventoryApi.createCount({ remark: '仓库盘点' })
  ElMessage.success('已创建草稿盘点单')
  await loadCounts()
}

const openCount = async (row: any) => {
  const detail = await inventoryApi.countDetail(row.id)
  activeCount.value = detail.count
  countItems.value = detail.items
}

const startCount = async () => {
  if (!activeCount.value) return
  await inventoryApi.startCount(activeCount.value.id)
  ElMessage.success('已进入盘点中')
  await openCount(activeCount.value)
  await loadCounts()
}

const addCountItem = async () => {
  if (!activeCount.value) return
  const { value: batchId } = await ElMessageBox.prompt('批次 ID', '录入盘点项', {
    inputValidator: (v) => /^\d+$/.test(v) || '请输入批次 ID'
  })
  const { value: countedQty } = await ElMessageBox.prompt('实盘数量', '录入盘点项', {
    inputValue: '0',
    inputValidator: (v) => /^\d+$/.test(v) || '请输入非负整数'
  })
  const { value: reason } = await ElMessageBox.prompt('差异/确认原因', '录入盘点项', {
    inputValidator: (v) => !!v.trim() || '原因不能为空'
  })
  await inventoryApi.upsertCountItem(activeCount.value.id, {
    batchId: Number(batchId),
    countedQty: Number(countedQty),
    reason
  })
  ElMessage.success('盘点项已保存（账面量已快照）')
  await openCount(activeCount.value)
}

const completeCount = async () => {
  if (!activeCount.value) return
  await ElMessageBox.confirm('完成盘点将按账面快照与实盘差异追加 STOCK_COUNT 流水，且只能成功一次。', '完成盘点')
  await inventoryApi.completeCount(activeCount.value.id)
  ElMessage.success('盘点已完成')
  await openCount(activeCount.value)
  await load()
}

const cancelCount = async () => {
  if (!activeCount.value) return
  await inventoryApi.cancelCount(activeCount.value.id)
  ElMessage.success('盘点已取消')
  activeCount.value = null
  countItems.value = []
  await loadCounts()
}
</script>

<template>
  <div class="workbench">
    <h1>仓库库存闭环</h1>
    <p>近效期筛选、盘点差异确认、聚合库存对账；过期/隔离/不可售批次不进入近效期与可售对账口径。</p>
    <el-radio-group v-model="tab" style="margin-bottom: 16px">
      <el-radio-button label="batches">批次与近效期</el-radio-button>
      <el-radio-button label="counts">盘点单</el-radio-button>
      <el-radio-button label="recon">对账差异</el-radio-button>
    </el-radio-group>

    <template v-if="tab === 'batches'">
      <div class="toolbar">
        <el-radio-group v-model="expiryDays" @change="loadBatches">
          <el-radio-button :label="30">30 天</el-radio-button>
          <el-radio-button :label="60">60 天</el-radio-button>
          <el-radio-button :label="90">90 天</el-radio-button>
          <el-radio-button :label="0">全部批次</el-radio-button>
        </el-radio-group>
      </div>
      <h2>批次</h2>
      <el-table :data="batches">
        <el-table-column prop="batchNo" label="批号" />
        <el-table-column prop="medicineId" label="药品 ID" />
        <el-table-column prop="expiryDate" label="失效日期" />
        <el-table-column prop="availableQty" label="可用" />
        <el-table-column prop="reservedQty" label="预占" />
        <el-table-column prop="qualityStatus" label="质量状态" />
        <el-table-column prop="sellable" label="可售" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="warning" plain @click="adjust(row)">盘盈 / 盘亏</el-button>
          </template>
        </el-table-column>
      </el-table>
      <h2>最近流水</h2>
      <el-table :data="ledger">
        <el-table-column prop="eventNo" label="事件号" />
        <el-table-column prop="businessType" label="业务" />
        <el-table-column prop="batchId" label="批次 ID" />
        <el-table-column prop="availableDelta" label="可用变化" />
        <el-table-column prop="createTime" label="时间" />
      </el-table>
    </template>

    <template v-else-if="tab === 'counts'">
      <div class="toolbar">
        <el-button type="primary" @click="createCount">新建盘点单</el-button>
      </div>
      <el-table :data="counts" @row-click="openCount" highlight-current-row>
        <el-table-column prop="countNo" label="盘点单号" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
      <div v-if="activeCount" class="count-panel">
        <h2>盘点单 {{ activeCount.countNo }}（{{ activeCount.status }}）</h2>
        <div class="toolbar">
          <el-button v-if="activeCount.status === 'DRAFT'" @click="startCount">开始盘点</el-button>
          <el-button v-if="activeCount.status === 'DRAFT' || activeCount.status === 'COUNTING'" @click="addCountItem">录入盘点项</el-button>
          <el-button v-if="activeCount.status === 'COUNTING'" type="success" @click="completeCount">完成盘点</el-button>
          <el-button v-if="activeCount.status === 'DRAFT' || activeCount.status === 'COUNTING'" type="danger" plain @click="cancelCount">取消</el-button>
        </div>
        <el-table :data="countItems">
          <el-table-column prop="batchId" label="批次" />
          <el-table-column prop="medicineId" label="药品" />
          <el-table-column prop="bookQty" label="账面快照" />
          <el-table-column prop="countedQty" label="实盘" />
          <el-table-column prop="diffQty" label="差异" />
          <el-table-column prop="reason" label="原因" />
        </el-table>
      </div>
    </template>

    <template v-else>
      <p>只读对账：`medicine.stock` 与可售合格未过期批次可用量之和不一致时列出，不自动修正。</p>
      <el-button @click="loadRecon" style="margin-bottom: 12px">刷新差异</el-button>
      <el-table :data="mismatches">
        <el-table-column prop="medicineId" label="药品 ID" />
        <el-table-column prop="medicineName" label="药品名" />
        <el-table-column prop="aggregateStock" label="聚合库存" />
        <el-table-column prop="batchAvailableSum" label="可售批次合计" />
        <el-table-column prop="diffQty" label="差异" />
      </el-table>
    </template>
  </div>
</template>

<style scoped>
.workbench { max-width: 1200px; margin: 40px auto; padding: 24px; background: #fff; border-radius: 16px }
.workbench > p { color: #718198 }
.workbench h2 { margin-top: 28px }
.toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin: 12px 0 }
.count-panel { margin-top: 20px; padding-top: 8px; border-top: 1px solid #eef2f7 }
</style>
