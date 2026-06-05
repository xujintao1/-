<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
        </div>
      </template>

      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd(0)"><el-icon><Plus /></el-icon> 新增部门</el-button>
        <el-button @click="loadData" :loading="loading"><el-icon><Refresh /></el-icon> 刷新</el-button>
        <el-button @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id" :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="handleAdd(row.id)">新增</el-button>
            <el-button type="primary" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="deptOptions" placeholder="顶级部门"
            :props="{ label: 'deptName', value: 'id', children: 'children' }" check-strictly style="width: 100%" clearable />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { systemDeptApi } from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const deptOptions = ref([])
const isExpandAll = ref(true)

const form = reactive({ id: null, parentId: 0, deptName: '', leader: '', phone: '', sort: 0, status: 1 })
const rules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res = await systemDeptApi.tree()
    tableData.value = res.data || []
    deptOptions.value = [{ id: 0, deptName: '顶级部门', children: tableData.value }]
  } finally { loading.value = false }
}

const toggleExpandAll = () => { isExpandAll.value = !isExpandAll.value; loadData() }

const handleAdd = (parentId) => {
  dialogTitle.value = '新增部门'
  Object.assign(form, { id: null, parentId, deptName: '', leader: '', phone: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, { id: row.id, parentId: row.parentId, deptName: row.deptName, leader: row.leader, phone: row.phone, sort: row.sort, status: row.status })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) {
    await systemDeptApi.update(form)
  } else {
    await systemDeptApi.create(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' }).then(async () => {
    await systemDeptApi.remove(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.table-toolbar { margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
</style>
