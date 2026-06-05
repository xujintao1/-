<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按名称/联系人搜索" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增客户</el-button>
    </div>

    <el-table :data="list" border style="margin-top: 14px" v-loading="loading">
      <el-table-column prop="name" label="客户/企业名称" min-width="200" />
      <el-table-column prop="contactPerson" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="creditCode" label="统一社会信用代码" min-width="180" />
      <el-table-column label="意向" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.intentLevel" :type="levelType(row.intentLevel)">{{ row.intentLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="140">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑客户' : '新增客户'" width="520px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="客户/企业名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="信用代码"><el-input v-model="form.creditCode" /></el-form-item>
        <el-form-item label="意向等级">
          <el-select v-model="form.intentLevel" style="width: 100%">
            <el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerApi } from '../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '' })
const dialogVisible = ref(false)
const form = reactive({})

const levelType = (l) => ({ A: 'danger', B: 'warning', C: 'info' }[l] || '')

const load = async () => {
  loading.value = true
  try {
    const res = await customerApi.page(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const onPage = (p) => { query.current = p; load() }

const openCreate = () => { Object.keys(form).forEach((k) => delete form[k]); dialogVisible.value = true }
const openEdit = (row) => { Object.assign(form, row); dialogVisible.value = true }
const onSave = async () => {
  if (!form.name) { ElMessage.warning('请填写客户名称'); return }
  if (form.id) await customerApi.update(form.id, form)
  else await customerApi.create(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}
const onDelete = (row) => {
  ElMessageBox.confirm(`确认删除客户 ${row.name}?`, '提示', { type: 'warning' }).then(async () => {
    await customerApi.remove(row.id)
    ElMessage.success('已删除')
    load()
  })
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; }
</style>
