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

    <div v-else-if="nodes.length > 0" class="approval-flow-vertical">
      <div v-for="(node, idx) in nodes" :key="idx" class="flow-step">
        <div class="flow-step-left">
          <div class="flow-step-icon" :class="getIconClass(node)">
            <el-icon v-if="node.type === 0" size="16"><User /></el-icon>
            <el-icon v-else-if="node.status === 'approved'" size="16"><Check /></el-icon>
            <el-icon v-else-if="node.status === 'rejected'" size="16"><Close /></el-icon>
            <el-icon v-else-if="node.status === 'pending'" size="16"><Loading /></el-icon>
            <el-icon v-else size="16"><Clock /></el-icon>
          </div>
          <div v-if="idx < nodes.length - 1" class="flow-step-line"></div>
        </div>
        <div class="flow-step-right">
          <div class="flow-step-title">
            {{ node.nodeName }}
            <span v-if="node.statusLabel" class="status-tag" :class="'status-' + node.status">{{ node.statusLabel }}</span>
          </div>
          <div v-if="node.roleName" class="flow-step-approvers">
            <span class="approver-tag">{{ node.roleName }}</span>
          </div>
          <div v-if="node.comment" class="flow-step-comment">
            <span class="comment-label">审批意见：</span><span class="comment-text">{{ node.comment }}</span>
          </div>
          <div v-if="node.time" class="flow-step-time">{{ node.time }}</div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无审批记录" :image-size="60" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Loading, User, Check, Close, Clock } from '@element-plus/icons-vue'
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

// 将后端「流程 + 任务」转换为 hr 同款竖向流程节点（首节点为发起人）
const nodes = computed(() => {
  const list = []
  list.push({ type: 0, nodeName: '发起申请', status: 'start' })
  for (const t of tasks.value) {
    const status = mapTaskStatus(t.status)
    list.push({
      type: 1,
      nodeName: t.nodeName || '审批节点',
      roleName: roleName(t.approverRole),
      status,
      statusLabel: taskStatusMap[t.status] || t.status,
      comment: t.comment || '',
      time: t.handledTime || ''
    })
  }
  return list
})

const mapTaskStatus = (s) => {
  if (s === 'APPROVED') return 'approved'
  if (s === 'REJECTED') return 'rejected'
  if (s === 'PENDING') return 'pending'
  return 'waiting'
}

const getIconClass = (node) => {
  if (node.type === 0) return 'icon-start'
  if (node.status === 'approved') return 'icon-approved'
  if (node.status === 'rejected') return 'icon-rejected'
  if (node.status === 'pending') return 'icon-pending'
  return 'icon-waiting'
}

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
.loading-tip {
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}

/* 审批流程竖向样式（对齐 hr- 仓库 ApprovalTimeline） */
.approval-flow-vertical {
  padding: 8px 0;
}
.flow-step {
  display: flex;
  min-height: 60px;
}
.flow-step-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40px;
  flex-shrink: 0;
}
.flow-step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e6f4ff;
  color: #409eff;
  flex-shrink: 0;
}
.flow-step-icon.icon-start { background: #e6fffb; color: #13c2c2; }
.flow-step-icon.icon-approved { background: #f6ffed; color: #52c41a; }
.flow-step-icon.icon-pending { background: #fff7e6; color: #fa8c16; }
.flow-step-icon.icon-waiting { background: #f5f5f5; color: #999; }
.flow-step-icon.icon-rejected { background: #fff1f0; color: #f5222d; }
.flow-step-line {
  width: 2px;
  flex: 1;
  background: #e8e8e8;
  margin: 4px 0;
}
.flow-step-right {
  flex: 1;
  padding-left: 12px;
  padding-bottom: 16px;
}
.flow-step-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 32px;
}
.flow-step-approvers {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.approver-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
}
.status-tag.status-approved { background: #f6ffed; color: #52c41a; }
.status-tag.status-pending { background: #fff7e6; color: #fa8c16; }
.status-tag.status-waiting { background: #f5f5f5; color: #999; }
.status-tag.status-rejected { background: #fff1f0; color: #f5222d; }
.flow-step-comment {
  margin-top: 6px;
  padding: 6px 10px;
  background: #f9f9f9;
  border-radius: 4px;
  font-size: 13px;
}
.comment-label { color: #999; margin-right: 4px; }
.comment-text { color: #666; }
.flow-step-time {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
</style>
