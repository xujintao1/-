<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>OA流程配置</span>
          <el-button type="primary" @click="saveConfigs" :loading="saving">保存配置</el-button>
        </div>
      </template>

      <!-- OA对接配置 -->
      <el-divider content-position="left">OA系统对接配置</el-divider>
      <el-form label-width="180px" :model="configs" style="max-width: 800px;">
        <el-form-item label="启用OA审批">
          <el-switch v-model="configs.oa_enabled" active-value="true" inactive-value="false" />
          <span class="config-hint">启用后合同审批将提交至外部OA系统</span>
        </el-form-item>
        <el-form-item label="启用OA单点登录">
          <el-switch v-model="configs.oa_sso_enabled" active-value="true" inactive-value="false" />
          <span class="config-hint">启用后登录账号密码由 OA 系统统一校验；关闭则使用本系统本地账号</span>
        </el-form-item>
        <el-form-item label="OA API地址">
          <el-input v-model="configs.oa_api_url" placeholder="http://oa-host:8080/api" />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="configs.oa_callback_url" placeholder="审批完成后的回调地址（留空使用默认）" />
        </el-form-item>
        <el-form-item label="合同审批流程标识">
          <el-input v-model="configs.contract_workflow_type" placeholder="CONTRACT_APPROVAL">
            <template #prepend>流程标识</template>
          </el-input>
        </el-form-item>
      </el-form>

      <!-- 审批流程配置 -->
      <el-divider content-position="left">审批流程配置</el-divider>
      <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
        <template #title>审批网关决定流程走向：internal 为内置审批引擎，oa 为外部OA系统审批</template>
      </el-alert>
      <el-form label-width="180px" :model="configs" style="max-width: 800px;">
        <el-form-item label="审批网关">
          <el-radio-group v-model="configs.approval_gateway">
            <el-radio value="internal">内置审批</el-radio>
            <el-radio value="oa">OA审批</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="合同审批链">
          <el-input v-model="configs.contract_approval_chain" placeholder="SALES_MANAGER,FINANCE,LEGAL">
            <template #prepend>角色序列</template>
          </el-input>
          <span class="config-hint">逗号分隔，按顺序审批</span>
        </el-form-item>
      </el-form>

      <!-- 系统信息 -->
      <el-divider content-position="left">系统信息</el-divider>
      <el-form label-width="180px" :model="configs" style="max-width: 800px;">
        <el-form-item label="系统名称">
          <el-input v-model="configs.system_name" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const configs = reactive({
  oa_enabled: 'false',
  oa_sso_enabled: 'false',
  oa_api_url: '',
  oa_callback_url: '',
  contract_workflow_type: '',
  approval_gateway: 'internal',
  contract_approval_chain: '',
  system_name: ''
})

const load = async () => {
  loading.value = true
  try {
    const res = await configApi.all()
    const data = res.data || {}
    Object.values(data).flat().forEach((item) => {
      if (item.configKey in configs) {
        configs[item.configKey] = item.configValue || ''
      }
    })
  } finally { loading.value = false }
}

const saveConfigs = async () => {
  saving.value = true
  try {
    const updates = Object.entries(configs).map(([key, value]) => ({
      key,
      value: value == null ? '' : String(value)
    }))
    await configApi.batchUpdate(updates)
    ElMessage.success('配置已保存')
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; }
.config-hint { margin-left: 12px; color: #909399; font-size: 12px; }
</style>
