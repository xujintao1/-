<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按单元编号搜索" clearable style="width: 200px" @keyup.enter="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
        <el-option v-for="(v, k) in statusMap" :key="k" :label="v.text" :value="k" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="canManage" type="success" @click="openCreate">新增房源</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px" v-loading="loading">
      <el-table-column prop="unitNo" label="单元编号" width="110" />
      <el-table-column prop="projectName" label="产业园" min-width="150" />
      <el-table-column prop="buildingName" label="楼栋" width="90" />
      <el-table-column prop="floor" label="楼层" width="70" />
      <el-table-column prop="area" label="面积(㎡)" width="100" />
      <el-table-column prop="unitPrice" label="单价(元/㎡)" width="110" />
      <el-table-column prop="totalPrice" label="总价(元)" width="140">
        <template #default="{ row }">{{ Number(row.totalPrice).toLocaleString('zh-CN') }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" v-if="canManage">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 14px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.current"
      @current-change="onPage"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑房源' : '新增房源'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="产业园">
          <el-select v-model="form.projectId" placeholder="请选择" @change="onProjectChange" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="form.buildingId" placeholder="请选择" style="width: 100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单元编号"><el-input v-model="form.unitNo" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="form.floor" :min="1" /></el-form-item>
        <el-form-item label="面积(㎡)"><el-input-number v-model="form.area" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单价(元/㎡)"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="(v, k) in statusMap" :key="k" :label="v.text" :value="k" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { factoryUnitApi } from '../api'
import request from '../api/request'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const canManage = computed(() => auth.hasRole('ADMIN') || auth.hasRole('SALES_MANAGER'))

const statusMap = {
  ON_SALE: { text: '在售', type: 'success' },
  SUBSCRIBED: { text: '已认购', type: 'warning' },
  SIGNED: { text: '已签约', type: 'danger' },
  SOLD: { text: '已售', type: 'info' }
}

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', status: '' })

const dialogVisible = ref(false)
const form = reactive({})
const projects = ref([])
const buildings = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await factoryUnitApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const onPage = (p) => { query.current = p; load() }

const loadProjects = async () => {
  const res = await request.get('/projects')
  projects.value = res.data
}
const onProjectChange = async (pid) => {
  form.buildingId = null
  const res = await request.get(`/projects/${pid}/buildings`)
  buildings.value = res.data
}

const openCreate = () => {
  Object.keys(form).forEach((k) => delete form[k])
  form.status = 'ON_SALE'
  buildings.value = []
  dialogVisible.value = true
}
const openEdit = async (row) => {
  Object.assign(form, row)
  if (row.projectId) {
    const res = await request.get(`/projects/${row.projectId}/buildings`)
    buildings.value = res.data
  }
  dialogVisible.value = true
}
const onSave = async () => {
  if (form.id) {
    await factoryUnitApi.update(form.id, form)
  } else {
    await factoryUnitApi.create(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}
const onDelete = (row) => {
  ElMessageBox.confirm(`确认删除房源 ${row.unitNo}?`, '提示', { type: 'warning' }).then(async () => {
    await factoryUnitApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  })
}

onMounted(() => { load(); loadProjects() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
