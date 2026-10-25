<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover">
          <div class="kpi">
            <div class="kpi-icon" :style="{ background: card.color }">
              <el-icon :size="24" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="kpi-value">{{ card.value }}</div>
              <div class="kpi-title">{{ card.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>近 7 日门诊量趋势</span></template>
          <div ref="trendRef" class="trend-chart" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>住院概况</span></template>
          <el-descriptions :column="1" border size="small" v-if="inpatient">
            <el-descriptions-item label="在院人数">{{ inpatient.inHospital }}</el-descriptions-item>
            <el-descriptions-item label="床位使用率">{{ inpatient.bedUsage }}%（共 {{ inpatient.totalBeds }} 床）</el-descriptions-item>
            <el-descriptions-item label="平均住院日">{{ inpatient.avgStayDays }} 天</el-descriptions-item>
            <el-descriptions-item label="近30日出院">{{ inpatient.dischargedRecent }} 人次</el-descriptions-item>
          </el-descriptions>
          <el-skeleton v-else :rows="4" animated />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16">
      <h3>欢迎，{{ userStore.userInfo?.realName }}</h3>
      <p>当前角色：{{ userStore.userInfo?.roleName }}（{{ userStore.userInfo?.roleCode }}）</p>
      <p>门诊全流程已贯通：挂号 → 接诊 → 病历 → 处方 → 收费 → 发药 → 退药/退费。</p>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getOverview } from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const cards = ref([
  { title: '今日门诊量', value: '-', icon: 'Calendar', color: '#1f6feb' },
  { title: '在院人数', value: '-', icon: 'Bed', color: '#67c23a' },
  { title: '今日收入(元)', value: '-', icon: 'Money', color: '#e6a23c' },
  { title: '库存预警', value: '-', icon: 'Bell', color: '#f56c6c' }
])
const inpatient = ref(null)
const trendRef = ref()

onMounted(async () => {
  const res = await getOverview()
  const d = res.data
  cards.value[0].value = d.todayOutpatient
  cards.value[1].value = d.inHospital
  cards.value[2].value = d.todayIncome
  cards.value[3].value = d.stockWarn
  inpatient.value = d.inpatient
  await nextTick()
  const trend = d.trend7 || {}
  const chart = echarts.init(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(trend) },
    yAxis: { type: 'value', minInterval: 1 },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    series: [{ type: 'line', smooth: true, data: Object.values(trend),
      areaStyle: { opacity: 0.15 }, itemStyle: { color: '#1f6feb' } }]
  })
  window.addEventListener('resize', () => chart.resize())
})
</script>

<style scoped lang="scss">
.mt16 {
  margin-top: 16px;
}

.trend-chart {
  height: 260px;
  width: 100%;
}

.kpi {
  display: flex;
  align-items: center;
  gap: 14px;

  .kpi-icon {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .kpi-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  .kpi-title {
    font-size: 13px;
    color: #909399;
  }
}
</style>
