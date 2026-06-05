<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd(0)"><el-icon><Plus /></el-icon> 新增菜单</el-button>
        <el-button @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" row-key="id" :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" width="180" />
        <el-table-column prop="component" label="组件路径" width="160" />
        <el-table-column prop="menuType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.menuType]?.type">{{ typeMap[row.menuType]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="visible" label="显示" width="80">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="handleAdd(row.id)" v-if="row.menuType !== 'F'">新增</el-button>
            <el-button type="primary" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="menuOptions" placeholder="请选择上级菜单"
            :props="{ label: 'menuName', value: 'id', children: 'children' }" check-strictly style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.menuType !== 'F'">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.menuType !== 'F'">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.menuType === 'C'">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.menuType === 'F'">
          <el-input v-model="form.perms" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否显示" v-if="form.menuType !== 'F'">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
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
import { Plus } from '@element-plus/icons-vue'
import { systemMenuApi } from '../../api'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const menuOptions = ref([])
const isExpandAll = ref(true)

const typeMap = { M: { label: '目录', type: 'primary' }, C: { label: '菜单', type: 'success' }, F: { label: '按钮', type: 'warning' } }
const form = reactive({ id: null, parentId: 0, menuName: '', menuType: 'M', icon: '', path: '', component: '', perms: '', sort: 0, visible: 1 })
const rules = { menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res = await systemMenuApi.list()
    tableData.value = buildTree(res.data || [])
    menuOptions.value = [{ id: 0, menuName: '顶级菜单', children: tableData.value }]
  } finally { loading.value = false }
}

const buildTree = (list) => {
  const map = {}; const tree = []
  list.forEach(item => { map[item.id] = { ...item, children: [] } })
  list.forEach(item => {
    if (item.parentId === 0 || !item.parentId) { tree.push(map[item.id]) }
    else if (map[item.parentId]) { map[item.parentId].children.push(map[item.id]) }
  })
  return tree
}

const toggleExpandAll = () => { isExpandAll.value = !isExpandAll.value; loadData() }

const handleAdd = (parentId) => {
  dialogTitle.value = '新增菜单'
  Object.assign(form, { id: null, parentId, menuName: '', menuType: 'M', icon: '', path: '', component: '', perms: '', sort: 0, visible: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => { dialogTitle.value = '编辑菜单'; Object.assign(form, row); dialogVisible.value = true }

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.id) { await systemMenuApi.update(form) } else { await systemMenuApi.create(form) }
  ElMessage.success('操作成功'); dialogVisible.value = false; loadData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该菜单吗？', '提示', { type: 'warning' }).then(async () => {
    await systemMenuApi.remove(row.id); ElMessage.success('删除成功'); loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.table-toolbar { margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
</style>
