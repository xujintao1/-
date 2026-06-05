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

    <el-timeline v-else-if="tasks.length > 0" class="timeline-body">
      <el-timeline-item
        v-for="task in tasks"
        :key="task.id"
        :type="dotType(task)"
        :hollow="task.status === 'PENDING'"
        :timestamp="task.handledTime || ''"
        placement="top"
      >
        <div class="node-row">
          <span class="node-name">{{ task.nodeName }}</span>
          <el-tag size="small" type="info" class="role-tag">{{ roleName(task.approverRole) }}</el-tag>
          <el-tag size="small" :type="taskTagType(task.status)">{{ taskStatusName(task.status) }}</el-tag>
        </div>
        <div v-if="task.comment" class="node-comment">审批意见：{{ task.comment }}</div>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-else description="暂无审批记录" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { approvalApi } from '../api'

const props = defineProps({
  flowId: { type: [Number, String], default: null },
  autoLoad: { type: Boolean, default: true }
})

const loading = ref(false)
const flow = ref(null)
const tasks = ref([])

const roleMap = {
  ADMIN: '管理员', SALES: '销售', SALES_MANAGER: '销售经理',
  FINANCE: '财务', LEGAL: '法务', GM: '总经理'
}
const roleName = (r) => roleMap[r] || r || '-'

const flowStatusMap = {
  APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', WITHDRAWN: '已撤销'
}
const statusName = (s) => flowStatusMap[s] || s
const statusTagType = (s) => ({
  APPROVING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'info'
}[s] || 'info')

const taskStatusMap = {
  PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已作废'
}
const taskStatusName = (s) => taskStatusMap[s] || s
const taskTagType = (s) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info'
}[s] || 'info')
const dotType = (task) => ({
  PENDING: 'primary', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info'
}[task.status] || 'info')

const fetchDetail = async () => {
  if (!props.flowId) {
    flow.value = null
    tasks.value = []
    return
  }
  loading.value = true
  try {
    const res = await approvalApi.flowDetail(props.flowId)
    flow.value = res.data ? res.data.flow : null
    tasks.value = res.data ? (res.data.tasks || []) : []
  } catch (e) {
    flow.value = null
    tasks.value = []
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
.approval-timeline {
  padding: 4px 0;
}
.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
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
.timeline-body { padding-left: 4px; }
.node-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-name { font-weight: 500; color: #1f2d3d; }
.role-tag { margin-left: 2px; }
.node-comment {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}
.loading-tip {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}
</style>
