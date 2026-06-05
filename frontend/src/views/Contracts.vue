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
      <el-table-column label="操作" min-width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" size="small" @click="onSubmit(row)">提交审批</el-button>
          <el-button v-if="row.status === 'APPROVING'" link type="warning" size="small" @click="onWithdraw(row)">撤销</el-button>
          <el-button v-if="isAdmin && row.status === 'APPROVING'" link type="success" size="small" @click="onResync(row)">重新同步</el-button>
          <el-button v-if="row.status === 'REJECTED'" link type="primary" size="small" @click="onResubmit(row)">重新提交</el-button>
          <el-button v-if="row.status === 'REJECTED'" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          <el-button v-if="row.status === 'EFFECTIVE'" link type="success" size="small" @click="openPayment(row)">登记回款</el-button>
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
      <ApprovalFlowPreview biz-type="CONTRACT" />
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 合同详情（状态头部 + 合同信息 + 审批流程，对齐 hr- 详情布局） -->
    <el-dialog v-model="detailVisible" title="合同详情" width="680px">
      <div v-if="detailRow">
        <div class="detail-header">
          <el-tag :type="statusMap[detailRow.status]?.type" size="large">{{ statusMap[detailRow.status]?.text || detailRow.status }}</el-tag>
          <span class="meta">合同编号：{{ detailRow.contractNo }}</span>
          <span class="meta" v-if="detailRow.createTime">发起时间：{{ fmtTime(detailRow.createTime) }}</span>
        </div>
        <el-descriptions :column="2" border size="small" style="margin-top: 14px">
          <el-descriptions-item label="客户">{{ detailRow.customerName }}</el-descriptions-item>
          <el-descriptions-item label="厂房单元">{{ detailRow.unitNo }}</el-descriptions-item>
          <el-descriptions-item label="合同金额(元)">{{ fmt(detailRow.amount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠(元)">{{ fmt(detailRow.discount) }}</el-descriptions-item>
          <el-descriptions-item label="付款方式">{{ payMap[detailRow.paymentMethod] || detailRow.paymentMethod }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusMap[detailRow.status]?.text || detailRow.status }}</el-descriptions-item>
          <el-descriptions-item label="合同条款" :span="2">{{ detailRow.terms || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">审批流程</el-divider>
        <ApprovalTimeline v-if="detailRow.approvalFlowId" :flow-id="detailRow.approvalFlowId" ref="timelineRef" />
        <el-empty v-else description="该合同尚未提交审批" :image-size="60" />
      </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { contractApi, subscriptionApi, paymentApi, approvalApi } from '../api'
import { useAuthStore } from '../store/auth'
import ApprovalFlowPreview from '../components/ApprovalFlowPreview.vue'
import ApprovalTimeline from '../components/ApprovalTimeline.vue'

const auth = useAuthStore()
const isAdmin = computed(() => auth.roles.includes('ADMIN'))

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

const detailVisible = ref(false)
const detailRow = ref(null)

const paymentVisible = ref(false)
const payForm = reactive({})
const payments = ref([])
let currentContractId = null

const fmt = (v) => (v === undefined || v === null) ? 0 : Number(v).toLocaleString('zh-CN')
const fmtTime = (t) => {
  if (!t) return '-'
  if (Array.isArray(t)) {
    const [y, m, d, h = 0, min = 0, s = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t).replace('T', ' ').substring(0, 19)
}

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

const openDetail = (row) => {
  detailRow.value = row
  detailVisible.value = true
}

const onWithdraw = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入撤销原因（可选）', '撤销审批', {
      confirmButtonText: '确认撤销', cancelButtonText: '取消', inputType: 'textarea'
    })
    await approvalApi.withdraw(row.approvalFlowId, { comment: value })
    ElMessage.success('已撤销审批')
    load()
  } catch (e) { /* 用户取消 */ }
}

const onResubmit = async (row) => {
  try {
    await ElMessageBox.confirm('确认重新提交该合同审批？流程将从第一个节点重新开始。', '重新提交', { type: 'warning' })
    await approvalApi.resubmit(row.approvalFlowId)
    ElMessage.success('已重新提交审批')
    load()
  } catch (e) { /* 用户取消 */ }
}

// 重新同步（状态回调）：以 OA 端状态为准校准本地流程，仅管理员
const onResync = async (row) => {
  try {
    await ElMessageBox.confirm('将以 OA 端真实状态为准重新校准该合同的审批流。', '重新同步', { type: 'warning' })
    const res = await approvalApi.resync(row.approvalFlowId)
    ElMessage.success(res.data || '同步完成')
    load()
  } catch (e) { /* 用户取消 */ }
}

// 删除审批流（驳回后）：删除后合同回退草稿、解除关联
const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该合同的审批流？删除后合同将回退为草稿状态。', '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await approvalApi.remove(row.approvalFlowId)
    ElMessage.success('已删除审批流')
    load()
  } catch (e) { /* 用户取消 */ }
}

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
