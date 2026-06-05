<template>
  <el-card>
    <div class="toolbar">
      <el-button type="success" @click="openCreate">新建认购单</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px" v-loading="loading">
      <el-table-column prop="subscriptionNo" label="认购单号" width="180" />
      <el-table-column prop="customerName" label="客户" min-width="180" />
      <el-table-column prop="unitNo" label="厂房单元" width="110" />
      <el-table-column label="定金(元)" width="120">
        <template #default="{ row }">{{ fmt(row.deposit) }}</template>
      </el-table-column>
      <el-table-column label="成交总价(元)" width="150">
        <template #default="{ row }">{{ fmt(row.totalPrice) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" size="small" @click="onCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 14px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.size" :current-page="query.current" @current-change="onPage"
    />

    <el-dialog v-model="dialogVisible" title="新建认购单" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="客户" required>
          <el-select v-model="form.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="厂房单元" required>
          <el-select v-model="form.factoryUnitId" filterable placeholder="选择在售房源" style="width: 100%" @change="onUnitChange">
            <el-option v-for="u in units" :key="u.id" :label="`${u.unitNo}（${u.area}㎡ / ${fmt(u.totalPrice)}元）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成交总价"><el-input :model-value="fmt(selectedTotal)" disabled /></el-form-item>
        <el-form-item label="定金(元)" required><el-input-number v-model="form.deposit" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">提交</el-button>
      </template>
    </el-dialog>

    <!-- 认购单详情（状态头部 + 认购信息，对齐 hr- 详情布局） -->
    <el-dialog v-model="detailVisible" title="认购单详情" width="600px">
      <div v-if="detailRow">
        <div class="detail-header">
          <el-tag :type="statusMap[detailRow.status]?.type" size="large">{{ statusMap[detailRow.status]?.text || detailRow.status }}</el-tag>
          <span class="meta">认购单号：{{ detailRow.subscriptionNo }}</span>
          <span class="meta" v-if="detailRow.createTime">创建时间：{{ fmtTime(detailRow.createTime) }}</span>
        </div>
        <el-descriptions :column="2" border size="small" style="margin-top: 14px">
          <el-descriptions-item label="客户">{{ detailRow.customerName }}</el-descriptions-item>
          <el-descriptions-item label="厂房单元">{{ detailRow.unitNo }}</el-descriptions-item>
          <el-descriptions-item label="定金(元)">{{ fmt(detailRow.deposit) }}</el-descriptions-item>
          <el-descriptions-item label="成交总价(元)">{{ fmt(detailRow.totalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusMap[detailRow.status]?.text || detailRow.status }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailRow.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { subscriptionApi, customerApi, factoryUnitApi } from '../api'

const statusMap = {
  ACTIVE: { text: '生效中', type: 'success' },
  CONTRACTED: { text: '已签合同', type: 'warning' },
  CANCELLED: { text: '已取消', type: 'info' }
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const dialogVisible = ref(false)
const form = reactive({})
const customers = ref([])
const units = ref([])
const detailVisible = ref(false)
const detailRow = ref(null)

const fmt = (v) => (v === undefined || v === null) ? 0 : Number(v).toLocaleString('zh-CN')
const fmtTime = (t) => {
  if (!t) return '-'
  if (Array.isArray(t)) {
    const [y, m, d, h = 0, min = 0, s = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t).replace('T', ' ').substring(0, 19)
}
const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}
const selectedTotal = computed(() => {
  const u = units.value.find((x) => x.id === form.factoryUnitId)
  return u ? u.totalPrice : 0
})

const load = async () => {
  loading.value = true
  try {
    const res = await subscriptionApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { query.current = p; load() }

const openCreate = async () => {
  Object.keys(form).forEach((k) => delete form[k])
  const [cs, us] = await Promise.all([
    customerApi.all(),
    factoryUnitApi.page({ current: 1, size: 200, status: 'ON_SALE' })
  ])
  customers.value = cs.data
  units.value = us.data.records
  dialogVisible.value = true
}
const onUnitChange = () => {}
const onSave = async () => {
  if (!form.customerId || !form.factoryUnitId) { ElMessage.warning('请选择客户和房源'); return }
  await subscriptionApi.create(form)
  ElMessage.success('认购单创建成功')
  dialogVisible.value = false
  load()
}
const onCancel = (row) => {
  ElMessageBox.confirm(`确认取消认购单 ${row.subscriptionNo}? 房源将恢复在售。`, '提示', { type: 'warning' }).then(async () => {
    await subscriptionApi.cancel(row.id)
    ElMessage.success('已取消')
    load()
  })
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; }
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-header .meta {
  color: #606266;
  font-size: 13px;
}
</style>
