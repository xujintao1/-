<template>
  <el-card>
    <div class="toolbar">
      <el-button type="success" @click="openCreate">生成合同</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px" v-loading="loading">
      <el-table-column prop="contractNo" label="合同编号" width="180" />
      <el-table-column prop="customerName" label="客户" min-width="170" />
      <el-table-column prop="unitNo" label="厂房单元" width="100" />
      <el-table-column label="金额(元)" width="140">
        <template #default="{ row }">{{ fmt(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="优惠(元)" width="110">
        <template #default="{ row }">{{ fmt(row.discount) }}</template>
      </el-table-column>
      <el-table-column label="付款方式" width="100">
        <template #default="{ row }">{{ payMap[row.paymentMethod] || row.paymentMethod }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="240">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" size="small" type="primary" @click="onSubmit(row)">提交审批</el-button>
          <el-button size="small" @click="openProgress(row)">审批进度</el-button>
          <el-button v-if="row.status === 'EFFECTIVE'" size="small" type="success" @click="openPayment(row)">登记回款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 14px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total" :page-size="query.size" :current-page="query.current" @current-change="onPage"
    />

    <!-- 生成合同 -->
    <el-dialog v-model="createVisible" title="生成销售合同" width="540px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="认购单" required>
          <el-select v-model="form.subscriptionId" filterable placeholder="选择生效中的认购单" style="width: 100%" @change="onSubChange">
            <el-option v-for="s in activeSubs" :key="s.id" :label="`${s.subscriptionNo} | ${s.customerName} | ${s.unitNo}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成交总价"><el-input :model-value="fmt(selectedSubTotal)" disabled /></el-form-item>
        <el-form-item label="优惠金额(元)"><el-input-number v-model="form.discount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="付款方式">
          <el-select v-model="form.paymentMethod" style="width: 100%">
            <el-option label="全款" value="FULL" /><el-option label="分期" value="INSTALLMENT" /><el-option label="按揭" value="MORTGAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同条款"><el-input v-model="form.terms" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 审批进度 -->
    <el-dialog v-model="progressVisible" title="合同审批进度" width="560px">
      <el-steps :active="activeStep" finish-status="success" align-center v-if="tasks.length">
        <el-step v-for="t in tasks" :key="t.id" :title="t.nodeName" :status="stepStatus(t)" />
      </el-steps>
      <el-empty v-else description="该合同尚未提交审批" />
      <el-timeline style="margin-top: 20px" v-if="tasks.length">
        <el-timeline-item v-for="t in tasks" :key="t.id" :type="stepStatus(t) === 'success' ? 'success' : (stepStatus(t) === 'error' ? 'danger' : 'primary')"
          :timestamp="t.handledTime || ''">
          <b>{{ t.nodeName }}</b>（{{ roleName(t.approverRole) }}）— {{ taskStatusText(t.status) }}
          <div v-if="t.comment" style="color:#909399">意见：{{ t.comment }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- 登记回款 -->
    <el-dialog v-model="paymentVisible" title="登记回款" width="480px">
      <el-form :model="payForm" label-width="100px">
        <el-form-item label="回款类型">
          <el-select v-model="payForm.paymentType" style="width: 100%">
            <el-option label="定金" value="DEPOSIT" /><el-option label="首付" value="DOWN_PAYMENT" />
            <el-option label="分期" value="INSTALLMENT" /><el-option label="尾款" value="FINAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)" required><el-input-number v-model="payForm.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="回款日期"><el-date-picker v-model="payForm.paymentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="payForm.remark" /></el-form-item>
      </el-form>
      <el-divider>回款记录</el-divider>
      <el-table :data="payments" size="small" border>
        <el-table-column label="类型" width="90"><template #default="{ row }">{{ payTypeMap[row.paymentType] || row.paymentType }}</template></el-table-column>
        <el-table-column label="金额"><template #default="{ row }">{{ fmt(row.amount) }}</template></el-table-column>
        <el-table-column prop="paymentDate" label="日期" width="120" />
      </el-table>
      <template #footer>
        <el-button @click="paymentVisible = false">关闭</el-button>
        <el-button type="primary" @click="onPay">登记</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { contractApi, subscriptionApi, paymentApi } from '../api'

const statusMap = {
  DRAFT: { text: '草稿', type: 'info' },
  APPROVING: { text: '审批中', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  EFFECTIVE: { text: '已生效', type: 'success' }
}
const payMap = { FULL: '全款', INSTALLMENT: '分期', MORTGAGE: '按揭' }
const payTypeMap = { DEPOSIT: '定金', DOWN_PAYMENT: '首付', INSTALLMENT: '分期', FINAL: '尾款' }
const roleMap = { SALES_MANAGER: '销售经理', FINANCE: '财务', LEGAL: '法务', GM: '总经理' }
const roleName = (r) => roleMap[r] || r

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

const createVisible = ref(false)
const form = reactive({})
const activeSubs = ref([])
const selectedSubTotal = computed(() => {
  const s = activeSubs.value.find((x) => x.id === form.subscriptionId)
  return s ? s.totalPrice : 0
})

const progressVisible = ref(false)
const tasks = ref([])
const activeStep = computed(() => tasks.value.filter((t) => t.status === 'APPROVED').length)

const paymentVisible = ref(false)
const payForm = reactive({})
const payments = ref([])
let currentContractId = null

const fmt = (v) => (v === undefined || v === null) ? 0 : Number(v).toLocaleString('zh-CN')

const load = async () => {
  loading.value = true
  try {
    const res = await contractApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { query.current = p; load() }

const openCreate = async () => {
  Object.keys(form).forEach((k) => delete form[k])
  form.paymentMethod = 'FULL'
  form.discount = 0
  const res = await subscriptionApi.page({ current: 1, size: 200 })
  activeSubs.value = res.data.records.filter((s) => s.status === 'ACTIVE')
  createVisible.value = true
}
const onSubChange = () => {}
const onCreate = async () => {
  if (!form.subscriptionId) { ElMessage.warning('请选择认购单'); return }
  await contractApi.create(form)
  ElMessage.success('合同已生成（草稿）')
  createVisible.value = false
  load()
}
const onSubmit = async (row) => {
  await contractApi.submit(row.id)
  ElMessage.success('已提交审批')
  load()
}

const openProgress = async (row) => {
  const res = await contractApi.approvalTasks(row.id)
  tasks.value = res.data
  progressVisible.value = true
}
const stepStatus = (t) => {
  if (t.status === 'APPROVED') return 'success'
  if (t.status === 'REJECTED') return 'error'
  return 'wait'
}
const taskStatusText = (s) => ({ PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)

const openPayment = async (row) => {
  currentContractId = row.id
  Object.keys(payForm).forEach((k) => delete payForm[k])
  payForm.paymentType = 'DEPOSIT'
  const res = await paymentApi.listByContract(row.id)
  payments.value = res.data
  paymentVisible.value = true
}
const onPay = async () => {
  if (!payForm.amount) { ElMessage.warning('请填写金额'); return }
  await paymentApi.create({ ...payForm, contractId: currentContractId })
  ElMessage.success('回款已登记')
  const res = await paymentApi.listByContract(currentContractId)
  payments.value = res.data
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; }
</style>
