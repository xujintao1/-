<template>
  <el-card v-loading="loading">
    <div class="toolbar">
      <el-button type="success" @click="openCreate">新增角色</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px">
      <el-table-column prop="code" label="角色编码" width="180" />
      <el-table-column prop="name" label="角色名称" width="180" />
      <el-table-column prop="description" label="描述" min-width="240" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 SALES" />
        </el-form-item>
        <el-form-item label="角色名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
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
import { systemRoleApi } from '../../api'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({})

const load = async () => {
  loading.value = true
  try {
    const res = await systemRoleApi.list()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}

const onSubmit = async () => {
  if (!form.code || !form.name) { ElMessage.warning('请填写角色编码和名称'); return }
  if (form.id) {
    await systemRoleApi.update(form.id, form)
    ElMessage.success('已更新')
  } else {
    await systemRoleApi.create(form)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  load()
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '提示', { type: 'warning' })
    await systemRoleApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; }
</style>
