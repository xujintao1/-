<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-tag type="info" size="small">数据同步自OA系统</el-tag>
        </div>
      </template>

      <div class="table-toolbar">
        <el-button @click="loadData" :loading="loading"><el-icon><Refresh /></el-icon> 刷新数据</el-button>
        <el-button @click="toggleExpandAll">{{ isExpandAll ? '折叠全部' : '展开全部' }}</el-button>
        <el-text type="info" size="small" style="margin-left: 16px;">
          如需修改部门信息，请前往OA系统操作
        </el-text>
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
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { systemDeptApi } from '../../api'

const loading = ref(false)
const tableData = ref([])
const isExpandAll = ref(true)

const loadData = async () => {
  loading.value = true
  try {
    const res = await systemDeptApi.tree()
    tableData.value = res.data || []
  } finally { loading.value = false }
}

const toggleExpandAll = () => { isExpandAll.value = !isExpandAll.value; loadData() }

onMounted(() => loadData())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
</style>
