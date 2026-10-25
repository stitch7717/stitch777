<template>
  <el-card shadow="never">
    <el-tabs v-model="tab" @tab-change="renderCharts">
      <el-tab-pane label="门诊统计" name="outpatient">
        <div class="filter-bar">
          <el-date-picker v-model="opRange" type="daterange" value-format="YYYY-MM-DD" style="width: 260px" />
          <el-button type="primary" icon="Search" @click="loadOutpatient">查询</el-button>
        </div>
        <el-row :gutter="16">
          <el-col :span="14"><div ref="opTrendRef" class="chart" /></el-col>
          <el-col :span="10"><div ref="opPieRef" class="chart" /></el-col>
        </el-row>
        <el-alert type="info" :closable="false" class="mt8">
          <template #title>区间门诊量：{{ opData.total }} 人次</template>
        </el-alert>
      </el-tab-pane>

      <el-tab-pane label="住院统计" name="inpatient">
        <el-row :gutter="16" class="mt8">
          <el-col :span="6" v-for="c in inCards" :key="c.label">
            <div class="stat-card">
              <div class="stat-value">{{ c.value }}</div>
              <div class="stat-label">{{ c.label }}</div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="收入统计" name="income">
        <div class="filter-bar">
          <el-date-picker v-model="incRange" type="daterange" value-format="YYYY-MM-DD" style="width: 260px" />
          <el-button type="primary" icon="Search" @click="loadIncome">查询</el-button>
        </div>
        <el-row :gutter="16">
          <el-col :span="12"><div ref="incTrendRef" class="chart" /></el-col>
          <el-col :span="12"><div ref="incPieRef" class="chart" /></el-col>
        </el-row>
        <el-alert type="info" :closable="false" class="mt8">
          <template #title>
            区间总收入 ￥{{ incData.total }}（含退款 ￥{{ incData.refund }}，共 {{ incData.count }} 笔）
          </template>
        </el-alert>
      </el-tab-pane>

      <el-tab-pane label="药品统计" name="drug">
        <div ref="drugBarRef" class="chart" />
      </el-tab-pane>

      <el-tab-pane label="患者分析" name="patient">
        <el-row :gutter="16">
          <el-col :span="10"><div ref="patAgeRef" class="chart" /></el-col>
          <el-col :span="7"><div ref="patGenderRef" class="chart" /></el-col>
          <el-col :span="7"><div ref="patRegionRef" class="chart" /></el-col>
        </el-row>
      </el-tab-pane>
      <!-- 自定义报表 -->
      <el-tab-pane label="自定义报表" name="custom">
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="panel">
              <div class="panel-head">已保存配置</div>
              <div v-for="c in customList" :key="c.id" class="cfg-item" @click="loadCustom(c.id)">
                <span>{{ c.reportName }}</span>
                <el-button link type="danger" size="small" @click.stop="doDeleteCustom(c.id)">删</el-button>
              </div>
              <div v-if="!customList.length" class="tip" style="padding: 10px">暂无保存的配置</div>
            </div>
          </el-col>
          <el-col :span="18">
            <el-form :inline="true" size="small">
              <el-form-item label="数据集">
                <el-select v-model="customForm.dataset" style="width: 130px" @change="onDatasetChange">
                  <el-option v-for="d in datasets" :key="d.key" :label="d.key" :value="d.key" />
                </el-select>
              </el-form-item>
              <el-form-item label="字段">
                <el-select v-model="customForm.fields" multiple placeholder="选展示字段" style="width: 280px">
                  <el-option v-for="f in currentFields" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
            </el-form>
            <div v-for="(cond, idx) in customForm.conditions" :key="idx" class="cond-row">
              <el-select v-model="cond.field" placeholder="字段" size="small" style="width: 120px">
                <el-option v-for="f in currentFields" :key="f" :label="f" :value="f" />
              </el-select>
              <el-select v-model="cond.op" placeholder="运算符" size="small" style="width: 90px">
                <el-option v-for="o in ops" :key="o" :label="o" :value="o" />
              </el-select>
              <el-input v-model="cond.value" placeholder="值" size="small" style="width: 120px" />
              <el-button link type="danger" size="small" @click="customForm.conditions.splice(idx, 1)">删</el-button>
            </div>
            <el-button link type="primary" size="small" icon="Plus"
              @click="customForm.conditions.push({ field: null, op: '=', value: '' })">添加条件</el-button>
            <div class="cond-row mt8">
              <el-button type="primary" size="small" @click="runCustom">执 行</el-button>
              <el-button size="small" @click="saveCustom">保存配置</el-button>
            </div>
            <el-table v-if="customRows.length" :data="customRows" border size="small" class="mt8" max-height="320">
              <el-table-column v-for="col in customCols" :key="col" :prop="col" :label="col" />
            </el-table>
            <el-empty v-else description="执行后显示结果" :image-size="50" />
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { reactive } from 'vue'
import {
  getOutpatientReport, getInpatientReport, getIncomeReport,
  getDrugReport, getPatientReport,
  getCustomDatasets, executeCustom, saveCustomReport,
  getCustomPage, getCustomDetail, deleteCustomReport
} from '@/api/system'

const tab = ref('outpatient')
const monthStart = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}
const opRange = ref([monthStart(), new Date().toISOString().slice(0, 10)])
const incRange = ref([monthStart(), new Date().toISOString().slice(0, 10)])
const opData = ref({})
const incData = ref({})
const inCards = ref([])

const opTrendRef = ref()
const opPieRef = ref()
const incTrendRef = ref()
const incPieRef = ref()
const drugBarRef = ref()
const patAgeRef = ref()
const patGenderRef = ref()
const patRegionRef = ref()

function init(el, option) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

async function loadOutpatient() {
  const [s, e] = opRange.value || []
  const res = await getOutpatientReport({ startDate: s, endDate: e })
  opData.value = res.data
  await nextTick()
  init(opTrendRef.value, {
    title: { text: '门诊量趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(res.data.trend || {}) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'line', smooth: true, data: Object.values(res.data.trend || {}),
      areaStyle: { opacity: 0.15 }, itemStyle: { color: '#1f6feb' } }]
  })
  init(opPieRef.value, {
    title: { text: '科室分布', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['35%', '65%'], data: res.data.deptDist }]
  })
}

async function loadInpatient() {
  const res = await getInpatientReport({})
  const d = res.data
  inCards.value = [
    { label: '在院人数', value: d.inHospital },
    { label: '床位使用率', value: d.bedUsage + '%' },
    { label: '近30日出院人次', value: d.dischargedRecent },
    { label: '平均住院日', value: d.avgStayDays + ' 天' }
  ]
}

async function loadIncome() {
  const [s, e] = incRange.value || []
  const res = await getIncomeReport({ startDate: s, endDate: e })
  incData.value = res.data
  await nextTick()
  init(incTrendRef.value, {
    title: { text: '收款笔数趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(res.data.trend || {}) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: Object.values(res.data.trend || {}), itemStyle: { color: '#67c23a' } }]
  })
  init(incPieRef.value, {
    title: { text: '收入构成', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '60%', data: Object.entries(res.data.byType || {})
      .map(([name, value]) => ({ name, value: Number(value) })) }]
  })
}

async function loadDrug() {
  const res = await getDrugReport({ startDate: opRange.value?.[0], endDate: opRange.value?.[1] })
  await nextTick()
  const top = (res.data.top10 || []).slice().reverse()
  init(drugBarRef.value, {
    title: { text: '药品销售 TOP10（区间销售额 ￥' + res.data.total + '）' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map((t) => t.name) },
    series: [{ type: 'bar', data: top.map((t) => Number(t.amount)), itemStyle: { color: '#e6a23c' } }]
  })
}

async function loadPatient() {
  const res = await getPatientReport()
  const d = res.data
  await nextTick()
  init(patAgeRef.value, {
    title: { text: '年龄分布', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '55%', data: Object.entries(d.ageDist || {})
      .map(([name, value]) => ({ name, value })) }]
  })
  init(patGenderRef.value, {
    title: { text: '性别', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '55%', data: Object.entries(d.gender || {})
      .map(([name, value]) => ({ name, value })) }]
  })
  init(patRegionRef.value, {
    title: { text: '地域TOP5', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: 60 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: (d.regionTop || []).map((r) => r.name) },
    series: [{ type: 'bar', data: (d.regionTop || []).map((r) => r.value), itemStyle: { color: '#1f6feb' } }]
  })
}

// ---------- 自定义报表 ----------
const datasets = ref([])
const customList = ref([])
const customRows = ref([])
const customCols = ref([])
const customForm = reactive({ dataset: 'charge', fields: [], conditions: [] })
const ops = ['=', '!=', 'like', '>', '<', '>=', '<=']
const currentFields = ref([])

function onDatasetChange(key) {
  const d = datasets.value.find((x) => x.key === key)
  currentFields.value = d ? d.fields : []
  customForm.fields = []
  customForm.conditions = []
}

async function runCustom() {
  if (!customForm.fields.length) {
    ElMessage.warning('请选择展示字段')
    return
  }
  const res = await executeCustom(customForm)
  customRows.value = res.data.rows
  customCols.value = customForm.fields
}

async function saveCustom() {
  const name = prompt('请输入报表名称')
  if (!name) return
  await saveCustomReport({ reportName: name, config: customForm })
  ElMessage.success('配置已保存')
  loadCustomList()
}

async function loadCustomList() {
  const res = await getCustomPage({ pageNum: 1, pageSize: 50 })
  customList.value = res.data.list
}

async function loadCustom(id) {
  const res = await getCustomDetail(id)
  Object.assign(customForm, res.data.config)
  onDatasetChange(customForm.dataset)
  runCustom()
}

async function doDeleteCustom(id) {
  await deleteCustomReport(id)
  ElMessage.success('已删除')
  loadCustomList()
}

function renderCharts(name) {
  if (name === 'outpatient') loadOutpatient()
  if (name === 'inpatient') loadInpatient()
  if (name === 'income') loadIncome()
  if (name === 'drug') loadDrug()
  if (name === 'patient') loadPatient()
}

onMounted(async () => {
  loadOutpatient()
  const res = await getCustomDatasets()
  datasets.value = res.data.datasets
  onDatasetChange(customForm.dataset)
  loadCustomList()
})
</script>

<style scoped>
.panel {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.panel-head {
  padding: 8px 12px;
  background: #f5f7fa;
  font-weight: 600;
  font-size: 13px;
}

.cfg-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  border-bottom: 1px solid #f0f2f5;
}

.cfg-item:hover {
  background: #f5f7fa;
}

.cond-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 6px;
}

.mt8 {
  margin-top: 8px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.chart {
  height: 320px;
  width: 100%;
}

.mt8 {
  margin-top: 8px;
}

.stat-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f6feb;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 6px;
}
</style>
