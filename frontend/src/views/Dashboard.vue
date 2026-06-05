<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 16px">
      <template #header>房源状态分布</template>
      <div ref="chartRef" style="height: 360px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '../api'

const summary = reactive({})
const chartRef = ref()
let chart = null

const fmt = (v) => (v === undefined || v === null) ? 0 : Number(v).toLocaleString('zh-CN')

const cards = computed(() => [
  { title: '厂房总数', value: summary.totalUnits ?? 0, color: '#409eff' },
  { title: '在售', value: summary.onSaleUnits ?? 0, color: '#67c23a' },
  { title: '已认购', value: summary.subscribedUnits ?? 0, color: '#e6a23c' },
  { title: '已签约', value: summary.signedUnits ?? 0, color: '#f56c6c' },
  { title: '客户数', value: summary.customerCount ?? 0, color: '#909399' },
  { title: '认购单数', value: summary.subscriptionCount ?? 0, color: '#409eff' },
  { title: '生效合同额(元)', value: fmt(summary.effectiveContractAmount), color: '#67c23a' },
  { title: '累计回款(元)', value: fmt(summary.totalReceived), color: '#e6a23c' }
])

const renderChart = (data) => {
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      data,
      label: { formatter: '{b}: {c}' }
    }]
  })
}

const onResize = () => chart && chart.resize()

const load = async () => {
  const res = await dashboardApi.summary()
  Object.assign(summary, res.data)
  renderChart(res.data.unitStatusDistribution || [])
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<style scoped>
.stat-card { margin-bottom: 16px; }
.stat-title { color: #909399; font-size: 14px; }
.stat-value { font-size: 26px; font-weight: 700; margin-top: 8px; }
</style>
