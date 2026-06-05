<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-button type="danger" plain @click="handleClear">清空日志</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="queryParams.module" placeholder="操作模块" clearable style="width: 160px" />
        <el-input v-model="queryParams.operName" placeholder="操作人" clearable style="width: 160px" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="operType" label="操作类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.operType)">{{ row.operType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="操作描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方式" width="90" />
        <el-table-column prop="requestUri" label="请求地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="operName" label="操作人" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时" width="90">
          <template #default="{ row }">{{ row.costTime || 0 }}ms</template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="170" />
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.current" v-model:page-size="queryParams.size"
          :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { operLogApi } from '../../api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({ current: 1, size: 10, module: '', operName: '' })

const typeColor = (t) => {
  if (t === 'CREATE') return 'success'
  if (t === 'DELETE') return 'danger'
  if (t === 'UPDATE') return 'warning'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await operLogApi.page(queryParams)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const resetQuery = () => {
  queryParams.current = 1
  queryParams.module = ''
  queryParams.operName = ''
  loadData()
}

const handleClear = () => {
  ElMessageBox.confirm('确定要清空所有操作日志吗？此操作不可恢复。', '警告', { type: 'warning' }).then(async () => {
    await operLogApi.clear()
    ElMessage.success('已清空')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-bar { margin-bottom: 16px; display: flex; gap: 8px; align-items: center; }
.pagination-container { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
