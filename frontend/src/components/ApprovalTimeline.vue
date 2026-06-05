<template>
  <div class="approval-timeline">
    <div class="timeline-header" v-if="flow">
      <span class="label">审批状态：</span>
      <el-tag :type="statusTagType(flow.status)" size="small">{{ statusName(flow.status) }}</el-tag>
      <el-tag v-if="flow.gateway" size="small" type="info" class="gw-tag">
        {{ flow.gateway === 'oa' ? '外部 OA 审批' : '内置审批流' }}
      </el-tag>
      <span v-if="flow.externalNo" class="ext-no">实例号：{{ flow.externalNo }}</span>
    </div>

    <div v-if="loading" class="loading-tip">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>

    <ApprovalFlowSteps v-else :nodes="nodes" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { approvalApi } from '../api'
import ApprovalFlowSteps from './ApprovalFlowSteps.vue'

const props = defineProps({
  flowId: { type: [Number, String], default: null },
  autoLoad: { type: Boolean, default: true }
})

const loading = ref(false)
const flow = ref(null)
const nodes = ref([])

const roleMap = {
  ADMIN: '管理员', SALES: '销售', SALES_MANAGER: '销售经理',
  FINANCE: '财务', LEGAL: '法务', GM: '总经理'
}
const roleName = (r) => roleMap[r] || r || '审批人'

const flowStatusMap = {
  APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤销'
}
const statusName = (s) => flowStatusMap[s] || s
const statusTagType = (s) => ({
  APPROVING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'info'
}[s] || 'info')

// 将「审批流 + 节点任务」映射为 hr- 垂直流程节点
const buildNodes = (flowData, tasks) => {
  const result = []
  result.push({
    type: 0,
    nodeName: '发起申请',
    approvers: [{ name: '发起人' }],
    status: 'approved',
    statusName: '已提交'
  })
  const sorted = [...(tasks || [])].sort((a, b) => (a.step || 0) - (b.step || 0))
  for (const task of sorted) {
    let status = 'waiting'
    let label = '待审批'
    if (task.status === 'APPROVED') { status = 'approved'; label = '已通过' }
    else if (task.status === 'REJECTED') { status = 'rejected'; label = '已驳回' }
    else if (task.status === 'CANCELLED') { status = 'waiting'; label = '已作废' }
    else if (task.status === 'PENDING') {
      if (flowData && flowData.status === 'APPROVING' && task.step === flowData.currentStep) {
        status = 'pending'; label = '审批中'
      } else {
        status = 'waiting'; label = '待审批'
      }
    }
    result.push({
      type: 1,
      nodeName: task.nodeName || '审批节点',
      approvers: [{ name: roleName(task.approverRole) }],
      status,
      statusName: label,
      comment: task.comment || '',
      completeTime: task.handledTime || ''
    })
  }
  return result
}

const fetchDetail = async () => {
  if (!props.flowId) {
    flow.value = null
    nodes.value = []
    return
  }
  loading.value = true
  try {
    const res = await approvalApi.flowDetail(props.flowId)
    flow.value = res.data ? res.data.flow : null
    nodes.value = res.data ? buildNodes(res.data.flow, res.data.tasks) : []
  } catch (e) {
    flow.value = null
    nodes.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.flowId, () => {
  if (props.autoLoad) fetchDetail()
}, { immediate: true })

defineExpose({ refresh: fetchDetail })
</script>

<style scoped>
.approval-timeline { padding: 4px 0; }
.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.timeline-header .label {
  color: #606266;
  font-size: 14px;
}
.gw-tag { margin-left: 4px; }
.ext-no {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
.loading-tip {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
  text-align: center;
}
</style>
