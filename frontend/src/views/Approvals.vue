<template>
  <el-card>
    <template #header>
      <span>我的审批待办</span>
      <el-button style="float: right" size="small" @click="load">刷新</el-button>
    </template>

    <el-table :data="list" border v-loading="loading">
      <el-table-column prop="bizTitle" label="合同编号" width="190" />
      <el-table-column prop="nodeName" label="审批节点" width="150" />
      <el-table-column label="所需角色" width="120">
        <template #default="{ row }">{{ roleName(row.approverRole) }}</template>
      </el-table-column>
      <el-table-column prop="step" label="步骤" width="80" />
      <el-table-column label="操作" min-width="250">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="openAction(row, 'approve')">通过</el-button>
          <el-button size="small" type="danger" @click="openAction(row, 'reject')">驳回</el-button>
          <el-button size="small" @click="openFlow(row)">查看流程</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无待办审批" />

    <el-dialog v-model="flowVisible" title="审批流程" width="600px">
      <ApprovalTimeline v-if="currentFlowId" :flow-id="currentFlowId" />
      <el-empty v-else description="暂无流程信息" />
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="actionType === 'approve' ? '审批通过' : '审批驳回'" width="440px">
      <el-form label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="comment" type="textarea" :rows="3" :placeholder="actionType === 'approve' ? '可填写通过意见' : '请填写驳回原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="actionType === 'approve' ? 'success' : 'danger'" @click="submit">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { approvalApi } from '../api'
import ApprovalTimeline from '../components/ApprovalTimeline.vue'

const roleMap = { SALES_MANAGER: '销售经理', FINANCE: '财务', LEGAL: '法务', GM: '总经理', ADMIN: '管理员' }
const roleName = (r) => roleMap[r] || r

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const actionType = ref('approve')
const comment = ref('')
let currentTask = null
const flowVisible = ref(false)
const currentFlowId = ref(null)

const openFlow = (row) => {
  currentFlowId.value = row.flowId || null
  flowVisible.value = true
}

const load = async () => {
  loading.value = true
  try {
    const res = await approvalApi.todo()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const openAction = (row, type) => {
  currentTask = row
  actionType.value = type
  comment.value = ''
  dialogVisible.value = true
}

const submit = async () => {
  if (actionType.value === 'reject' && !comment.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (actionType.value === 'approve') {
    await approvalApi.approve(currentTask.id, { comment: comment.value })
    ElMessage.success('已通过')
  } else {
    await approvalApi.reject(currentTask.id, { comment: comment.value })
    ElMessage.success('已驳回')
  }
  dialogVisible.value = false
  load()
}

onMounted(load)
</script>
