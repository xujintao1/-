<template>
  <div class="approval-flow-vertical" v-if="nodes.length > 0">
    <div v-for="(node, idx) in nodes" :key="idx" class="flow-step" :class="{ skipped: node.skip }">
      <div class="flow-step-left">
        <div class="flow-step-icon" :class="getIconClass(node)">
          <el-icon v-if="node.type === 0" size="16"><User /></el-icon>
          <el-icon v-else-if="node.status === 'approved'" size="16"><Check /></el-icon>
          <el-icon v-else-if="node.status === 'rejected'" size="16"><Close /></el-icon>
          <el-icon v-else-if="node.status === 'pending'" size="16"><Loading /></el-icon>
          <el-icon v-else-if="node.status === 'waiting'" size="16"><Clock /></el-icon>
          <el-icon v-else size="16"><Check /></el-icon>
        </div>
        <div v-if="idx < nodes.length - 1" class="flow-step-line"></div>
      </div>
      <div class="flow-step-right">
        <div class="flow-step-title">{{ node.nodeName || '节点' }}</div>
        <div class="flow-step-approvers">
          <span v-for="(a, aIdx) in (node.approvers || [])" :key="aIdx" class="approver-tag">
            {{ a.name || a.realName }}
          </span>
          <span v-if="node.statusName" class="status-tag" :class="'status-' + node.status">{{ node.statusName }}</span>
        </div>
        <div v-if="node.comment" class="flow-step-comment">
          <span class="comment-label">意见：</span>
          <span class="comment-text">{{ node.comment }}</span>
        </div>
        <div v-if="node.completeTime" class="flow-step-time">
          <span class="time-text">{{ formatTime(node.completeTime) }}</span>
        </div>
      </div>
    </div>
  </div>
  <el-empty v-else description="暂无审批流程" :image-size="60" />
</template>

<script setup>
import { Check, Close, Clock, User, Loading } from '@element-plus/icons-vue'

defineProps({
  nodes: { type: Array, default: () => [] }
})

const getIconClass = (node) => {
  if (node.type === 0) return 'icon-start'
  const s = node.status
  if (s === 'approved') return 'icon-approved'
  if (s === 'rejected') return 'icon-rejected'
  if (s === 'pending') return 'icon-pending'
  if (s === 'waiting') return 'icon-waiting'
  return 'icon-approve'
}

const formatTime = (time) => {
  if (!time) return ''
  if (Array.isArray(time)) {
    const [y, m, d, h = 0, min = 0, s = 0] = time
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(time).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
/* 审批流程垂直样式（对齐 hr- 仓库） */
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
.flow-step-icon.icon-approve { background: #e6f4ff; color: #409eff; }
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
.flow-step.skipped { opacity: 0.55; }
.flow-step.skipped .flow-step-title {
  text-decoration: line-through;
  color: #b0b3b8;
}
</style>
