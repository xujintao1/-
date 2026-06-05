<template>
  <div class="page-container">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-tag type="info" size="small">用户数据同步自OA系统，可在此分配角色与权限</el-tag>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="queryParams.keyword" placeholder="用户名/姓名" clearable style="width: 200px" @keyup.enter="load" />
        <el-tree-select v-model="queryParams.deptId" :data="deptTree" placeholder="部门" clearable
          :props="{ label: 'deptName', value: 'id', children: 'children' }" style="width: 200px" />
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button @click="load" :loading="loading"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>

      <el-table :data="list" border stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="200">
          <template #default="{ row }">
            <template v-if="splitRoles(row.roles).length">
              <el-tag v-for="r in splitRoles(row.roles)" :key="r" size="small" class="role-tag">{{ roleName(r) }}</el-tag>
            </template>
            <el-tag v-else type="info">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑用户：仅分配角色 + 启用状态，身份信息来自 OA 只读 -->
    <el-dialog v-model="dialogVisible" title="编辑用户授权" width="460px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :value="current.realName || current.username" disabled />
        </el-form-item>
        <el-form-item label="部门">
          <el-input :value="current.deptName" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoles" multiple style="width: 100%" placeholder="选择角色" clearable>
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" inline-prompt />
          <span style="margin-left: 12px; color: #909399; font-size: 12px;">禁用后该用户无法登录本系统</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit" :loading="saving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { systemUserApi, systemRoleApi, systemDeptApi } from '../../api'

const roleNameMap = {
  ADMIN: '管理员', SALES: '销售', SALES_MANAGER: '销售经理',
  FINANCE: '财务', LEGAL: '法务', GM: '总经理'
}
const roleName = (r) => roleNameMap[r] || r
const splitRoles = (s) => (s ? s.split(',').map((x) => x.trim()).filter(Boolean) : [])

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const roles = ref([])
const deptTree = ref([])

const queryParams = reactive({ keyword: '', deptId: null })

const dialogVisible = ref(false)
const current = ref({})
const form = reactive({ enabled: 1 })
const selectedRoles = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await systemUserApi.list({ keyword: queryParams.keyword, deptId: queryParams.deptId })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  const res = await systemRoleApi.list()
  roles.value = res.data || []
}

const loadDept = async () => {
  const res = await systemDeptApi.tree()
  deptTree.value = res.data || []
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.deptId = null
  load()
}

const openEdit = (row) => {
  current.value = row
  selectedRoles.value = splitRoles(row.roles)
  form.enabled = row.enabled === 1 ? 1 : 0
  dialogVisible.value = true
}

const onSubmit = async () => {
  saving.value = true
  try {
    await systemUserApi.assignRole({
      username: current.value.username,
      oaUserId: current.value.oaUserId,
      realName: current.value.realName,
      phone: current.value.phone,
      deptName: current.value.deptName,
      roles: selectedRoles.value.join(','),
      enabled: form.enabled
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  loadRoles()
  loadDept()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.role-tag {
  margin-right: 6px;
}
</style>
