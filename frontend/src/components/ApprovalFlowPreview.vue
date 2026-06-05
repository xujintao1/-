<template>
  <div class="approval-flow-preview">
    <div class="timeline-section">
      <div class="section-header">
        <h4>审批流程预览</h4>
        <el-tag v-if="processName" type="info" size="small">{{ processName }}</el-tag>
      </div>
      <div v-if="loading" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <ApprovalFlowSteps v-else :nodes="nodes" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { approvalApi } from '../api'
import ApprovalFlowSteps from './ApprovalFlowSteps.vue'

const props = defineProps({
  bizType: { type: String, default: 'CONTRACT' },
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

defineExpose({ refresh: fetchPreview })
</script>

<style scoped>
.approval-flow-preview { margin-top: 8px; }
.timeline-section { margin-top: 8px; }
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.timeline-section h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.loading-tip {
  text-align: center;
  color: #909399;
  padding: 20px 0;
}
</style>
