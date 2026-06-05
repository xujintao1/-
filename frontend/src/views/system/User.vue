<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/姓名" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增用户</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px">
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="realName" label="姓名" width="140" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="r in splitRoles(row.roles)" :key="r" size="small" class="role-tag">{{ roleName(r) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.enabled === 1 ? 'warning' : 'success'" @click="onToggle(row)">
            {{ row.enabled === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" @click="onReset(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoles" multiple style="width: 100%" placeholder="选择角色">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" placeholder="留空默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemUserApi, systemRoleApi } from '../../api'

const roleNameMap = {
  ADMIN: '管理员', SALES: '销售', SALES_MANAGER: '销售经理',
  FINANCE: '财务', LEGAL: '法务', GM: '总经理'
}
const roleName = (r) => roleNameMap[r] || r
const splitRoles = (s) => (s ? s.split(',').map((x) => x.trim()).filter(Boolean) : [])

const loading = ref(false)
const keyword = ref('')
const list = ref([])
const roles = ref([])

const dialogVisible = ref(false)
const form = reactive({})
const selectedRoles = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await systemUserApi.list(keyword.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  const res = await systemRoleApi.list()
  roles.value = res.data || []
}

const openCreate = () => {
  Object.keys(form).forEach((k) => delete form[k])
  form.enabled = 1
  selectedRoles.value = []
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  selectedRoles.value = splitRoles(row.roles)
  dialogVisible.value = true
}

const onSubmit = async () => {
  if (!form.username) { ElMessage.warning('请输入用户名'); return }
  const payload = { ...form, roles: selectedRoles.value.join(',') }
  if (form.id) {
    await systemUserApi.update(form.id, payload)
    ElMessage.success('已更新')
  } else {
    await systemUserApi.create(payload)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

const onToggle = async (row) => {
  await systemUserApi.setEnabled(row.id, row.enabled !== 1)
  ElMessage.success('状态已更新')
  load()
}

const onReset = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（留空则重置为 123456）', `重置「${row.username}」密码`, {
      confirmButtonText: '确认', cancelButtonText: '取消', inputValue: ''
    })
    const res = await systemUserApi.resetPassword(row.id, value)
    ElMessage.success(`密码已重置为：${res.data.password}`)
  } catch (e) { /* 取消 */ }
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
    await systemUserApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}

onMounted(() => { load(); loadRoles() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; }
.role-tag { margin-right: 4px; }
</style>
