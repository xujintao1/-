<template>
  <div class="approval-flow-preview">
    <el-card shadow="never" v-if="loading || nodes.length > 0">
      <template #header>
        <div class="card-header">
          <span>审批流程预览</span>
          <el-tag v-if="processName" type="info" size="small">{{ processName }}</el-tag>
        </div>
      </template>

      <el-skeleton :loading="loading" animated :rows="3">
        <template #default>
          <div class="flow-steps" v-if="nodes.length > 0">
            <el-steps :active="nodes.length" finish-status="wait" align-center>
              <el-step
                v-for="(node, index) in nodes"
                :key="index"
                :title="node.nodeName"
                :description="getNodeDescription(node)"
              >
                <template #icon>
                  <el-icon :class="getNodeIconClass(node.type)">
                    <component :is="getNodeIcon(node.type)" />
                  </el-icon>
                </template>
              </el-step>
            </el-steps>

            <div class="approvers-detail" v-if="showDetail">
              <el-divider content-position="left">审批节点详情</el-divider>
              <div v-for="(node, index) in nodes" :key="index" class="node-detail">
                <div class="node-header">
                  <el-tag :type="getNodeTagType(node.type)" size="small">
                    {{ getNodeTypeName(node.type) }}
                  </el-tag>
                  <span class="node-name">{{ node.nodeName }}</span>
                </div>
                <div class="node-approvers" v-if="node.approvers && node.approvers.length">
                  <span class="label">审批角色：</span>
                  <el-tag
                    v-for="(approver, i) in node.approvers"
                    :key="i"
                    size="small"
                    class="approver-tag"
                  >
                    {{ approver.name }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无审批流程信息" :image-size="60" />
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { User, Check, Bell, Share } from '@element-plus/icons-vue'
import { approvalApi } from '../api'

const props = defineProps({
  bizType: { type: String, default: 'CONTRACT' },
  showDetail: { type: Boolean, default: true },
  autoLoad: { type: Boolean, default: true }
})

const loading = ref(false)
const nodes = ref([])
const processName = ref('')

const fetchPreview = async () => {
  loading.value = true
  try {
    const res = await approvalApi.preview(props.bizType)
    if (res.data) {
      nodes.value = res.data.nodes || []
      processName.value = res.data.processName || ''
    }
  } catch (error) {
    console.error('获取审批流程预览失败:', error)
  } finally {
    loading.value = false
  }
}

watch(() => props.bizType, () => {
  if (props.autoLoad) fetchPreview()
}, { immediate: true })

const getNodeIcon = (type) => {
  switch (type) {
    case 0: return User
    case 1: return Check
    case 2: return Bell
    default: return Share
  }
}

const getNodeIconClass = (type) => {
  switch (type) {
    case 0: return 'icon-starter'
    case 1: return 'icon-approver'
    case 2: return 'icon-cc'
    default: return ''
  }
}

const getNodeTypeName = (type) => {
  switch (type) {
    case 0: return '发起人'
    case 1: return '审批人'
    case 2: return '抄送人'
    default: return '其他'
  }
}

const getNodeTagType = (type) => {
  switch (type) {
    case 0: return 'info'
    case 1: return 'primary'
    case 2: return 'warning'
    default: return 'info'
  }
}

const getNodeDescription = (node) => {
  if (node.approvers && node.approvers.length > 0) {
    return node.approvers.map(a => a.name).join(', ')
  }
  return ''
}

defineExpose({ refresh: fetchPreview })
</script>

<style scoped>
.approval-flow-preview {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.flow-steps {
  padding: 20px 0;
}
.icon-starter { color: #909399; }
.icon-approver { color: #409eff; }
.icon-cc { color: #e6a23c; }
.approvers-detail { margin-top: 20px; }
.node-detail {
  padding: 10px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.node-name { font-weight: 500; }
.node-approvers {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.node-approvers .label {
  color: #909399;
  font-size: 13px;
}
.approver-tag { margin-right: 4px; }
</style>
