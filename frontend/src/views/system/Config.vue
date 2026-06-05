<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>系统设置</span>
        <el-button type="primary" @click="onSave">保存配置</el-button>
      </div>
    </template>

    <el-tabs v-model="activeGroup">
      <el-tab-pane v-for="(label, group) in groupLabels" :key="group" :label="label" :name="group">
        <el-form label-width="180px" class="config-form">
          <el-form-item v-for="item in groupedConfigs[group] || []" :key="item.configKey" :label="item.description || item.configKey">
            <template v-if="item.valueType === 'boolean'">
              <el-switch v-model="item.configValue" active-value="true" inactive-value="false" />
            </template>
            <el-input v-else v-model="item.configValue" :placeholder="item.configKey" style="max-width: 460px" />
            <span class="config-key">{{ item.configKey }}</span>
          </el-form-item>
          <el-empty v-if="!(groupedConfigs[group] || []).length" description="暂无该分组配置" :image-size="60" />
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi } from '../../api'

const groupLabels = {
  oa_integration: 'OA 对接',
  workflow: '审批流程',
  system: '系统信息'
}

const loading = ref(false)
const activeGroup = ref('oa_integration')
const groupedConfigs = reactive({})

const load = async () => {
  loading.value = true
  try {
    const res = await configApi.all()
    const data = res.data || {}
    Object.keys(groupedConfigs).forEach((k) => delete groupedConfigs[k])
    Object.keys(data).forEach((group) => {
      groupedConfigs[group] = data[group]
    })
    // 默认选中第一个有数据的分组
    const firstGroup = Object.keys(groupLabels).find((g) => (groupedConfigs[g] || []).length)
    if (firstGroup) activeGroup.value = firstGroup
  } finally {
    loading.value = false
  }
}

const onSave = async () => {
  const updates = []
  Object.keys(groupedConfigs).forEach((group) => {
    (groupedConfigs[group] || []).forEach((item) => {
      updates.push({ group, key: item.configKey, value: item.configValue == null ? '' : String(item.configValue) })
    })
  })
  await configApi.batchUpdate(updates)
  ElMessage.success('配置已保存')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.config-form { padding-top: 10px; }
.config-key {
  margin-left: 12px;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
