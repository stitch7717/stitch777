<template>
  <el-card shadow="never">
    <el-form :inline="true" @submit.prevent>
      <el-form-item label="状态">
        <el-select v-model="status" style="width: 140px" @change="loadList">
          <el-option label="待缴费" :value="0" />
          <el-option label="待采样" :value="1" />
          <el-option label="检验中" :value="2" />
          <el-option label="已出报告" :value="3" />
          <el-option label="全部" :value="null" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目">
        <el-input v-model="examItem" placeholder="项目名称" clearable style="width: 150px" @keyup.enter="loadList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">查询</el-button>
        <el-button type="success" icon="Plus" @click="openApply">开检查申请</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="申请单号" prop="applyNo" min-width="150" />
      <el-table-column label="患者" prop="patientName" min-width="80" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">{{ row.examType === 1 ? '检验' : '检查' }}</template>
      </el-table-column>
      <el-table-column label="项目" prop="examItem" min-width="110" />
      <el-table-column label="费用" width="80" align="center">
        <template #default="{ row }">￥{{ row.fee }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="危急值" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isCritical === 1" size="small" type="danger">
            {{ row.criticalHandled === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="applyTime" min-width="150" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" link type="primary" @click="doSample(row)">采样</el-button>
          <el-button v-if="row.status === 2" link type="primary" @click="openReport(row)">录入结果</el-button>
          <el-button v-if="row.reportId && row.reportStatus === 0 && canAudit" link type="success"
            @click="doAudit(row)">审核</el-button>
          <el-button v-if="row.reportId && row.reportStatus === 0 && row.auditorId" link type="warning"
            @click="doPublish(row)">发布</el-button>
          <el-button v-if="row.reportId" link @click="openView(row)">查看报告</el-button>
          <el-button v-if="row.isCritical === 1 && row.criticalHandled !== 1 && canAudit" link type="danger"
            @click="openCritical(row)">危急值处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="pageNum" :total="total" layout="total, prev, pager, next" class="pager"
      @current-change="loadList" />

    <!-- 开申请 -->
    <el-dialog title="开检查申请" v-model="applyVisible" width="480px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="applyForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="`${p.name}（${p.patientNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="applyForm.examType">
            <el-radio :value="1">检验</el-radio>
            <el-radio :value="2">检查(影像)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目" required>
          <el-input v-model="applyForm.examItem" placeholder="如 血常规 / 胸部正位片" />
        </el-form-item>
        <el-form-item label="部位">
          <el-input v-model="applyForm.bodyPart" />
        </el-form-item>
        <el-form-item label="临床诊断">
          <el-input v-model="applyForm.clinicalDiagnosis" />
        </el-form-item>
        <el-form-item label="费用">
          <el-input-number v-model="applyForm.fee" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitApply">提 交</el-button>
      </template>
    </el-dialog>

    <!-- 录入结果 -->
    <el-dialog title="结果录入" v-model="reportVisible" width="720px" :close-on-click-modal="false">
      <el-table :data="reportForm.items" border size="small">
        <el-table-column label="项目" min-width="120">
          <template #default="{ row }"><el-input v-model="row.name" placeholder="WBC" /></template>
        </el-table-column>
        <el-table-column label="结果" min-width="90">
          <template #default="{ row }"><el-input v-model="row.value" /></template>
        </el-table-column>
        <el-table-column label="参考值" min-width="100">
          <template #default="{ row }"><el-input v-model="row.ref" placeholder="4-10" /></template>
        </el-table-column>
        <el-table-column label="单位" min-width="80">
          <template #default="{ row }"><el-input v-model="row.unit" /></template>
        </el-table-column>
        <el-table-column label="异常" width="60" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.abnormal" :true-value="1" :false-value="0" /></template>
        </el-table-column>
        <el-table-column label="" width="50" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="reportForm.items.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button link type="primary" icon="Plus" class="mt8" @click="reportForm.items.push({ name: '', value: '', ref: '', unit: '', abnormal: 0 })">
        添加明细
      </el-button>
      <el-form label-width="80px" class="mt8">
        <el-form-item label="结论"><el-input v-model="reportForm.conclusion" /></el-form-item>
        <el-form-item label="危急值">
          <el-radio-group v-model="reportForm.isCritical">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReport">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 查看报告 -->
    <el-drawer v-model="viewVisible" title="检验报告" size="560px">
      <template v-if="viewData">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报告号">{{ viewData.report?.reportNo }}</el-descriptions-item>
          <el-descriptions-item label="患者">{{ viewData.patientName }}</el-descriptions-item>
          <el-descriptions-item label="项目">{{ viewData.examItem }}</el-descriptions-item>
          <el-descriptions-item label="申请单">{{ viewData.applyNo }}</el-descriptions-item>
          <el-descriptions-item label="检验者">{{ viewData.reporterName }}</el-descriptions-item>
          <el-descriptions-item label="审核者">{{ viewData.auditorName }}</el-descriptions-item>
          <el-descriptions-item label="结论" :span="2">{{ viewData.report?.conclusion }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="viewData.items" border size="small" class="mt8">
          <el-table-column label="项目" prop="name" />
          <el-table-column label="结果" prop="value" />
          <el-table-column label="参考值" prop="ref" />
          <el-table-column label="单位" prop="unit" />
          <el-table-column label="异常" width="60" align="center">
            <template #default="{ row }">{{ row.abnormal === 1 ? '↑' : '' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <!-- 危急值处理 -->
    <el-dialog title="危急值处理" v-model="criticalVisible" width="420px">
      <el-form label-width="80px">
        <el-form-item label="处理备注" required>
          <el-input v-model="criticalRemark" type="textarea" :rows="3" placeholder="如：已通知管床医生，复查血钾" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="criticalVisible = false">取 消</el-button>
        <el-button type="danger" @click="submitCritical">确认处理</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getExamPage, createExamApply, examSample, saveExamReport, auditExamReport,
  publishExamReport, getExamReport, handleCritical, getPatientPage
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canAudit = computed(() => ['admin', 'doctor'].includes(userStore.userInfo?.roleCode))

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const status = ref(null)
const examItem = ref('')

async function loadList() {
  loading.value = true
  try {
    const res = await getExamPage({ pageNum: pageNum.value, pageSize: 10, status: status.value, examItem: examItem.value })
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function statusText(s) {
  return { 0: '待缴费', 1: '待采样', 2: '检验中', 3: '已出报告' }[s]
}
function statusType(s) {
  return { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success' }[s]
}

// ---------- 开申请 ----------
const applyVisible = ref(false)
const applyForm = reactive({ patientId: null, examType: 1, examItem: '', bodyPart: '', clinicalDiagnosis: '', fee: 30 })
const patientOptions = ref([])
const searching = ref(false)

function openApply() {
  Object.assign(applyForm, { patientId: null, examType: 1, examItem: '', bodyPart: '', clinicalDiagnosis: '', fee: 30 })
  applyVisible.value = true
}

async function searchPatients(kw) {
  if (!kw) return
  searching.value = true
  try {
    const res = await getPatientPage({ pageNum: 1, pageSize: 20, keyword: kw })
    patientOptions.value = res.data.list
  } finally {
    searching.value = false
  }
}

async function submitApply() {
  if (!applyForm.patientId || !applyForm.examItem) {
    ElMessage.warning('请选择患者并填写项目')
    return
  }
  const res = await createExamApply(applyForm)
  ElMessage.success(`已开单：${res.data.applyNo}`)
  applyVisible.value = false
  loadList()
}

async function doSample(row) {
  await examSample(row.id)
  ElMessage.success('已采集，进入检验中')
  loadList()
}

// ---------- 录入报告 ----------
const reportVisible = ref(false)
const reportForm = reactive({ applyId: null, conclusion: '', isCritical: 0, items: [] })

function openReport(row) {
  reportForm.applyId = row.id
  reportForm.conclusion = ''
  reportForm.isCritical = 0
  reportForm.items = [{ name: '', value: '', ref: '', unit: '', abnormal: 0 }]
  reportVisible.value = true
}

async function submitReport() {
  await saveExamReport(reportForm)
  ElMessage.success('报告已保存（草稿），可审核后发布')
  reportVisible.value = false
  loadList()
}

async function doAudit(row) {
  await auditExamReport(row.reportId)
  ElMessage.success('审核通过')
  loadList()
}

async function doPublish(row) {
  await publishExamReport(row.reportId)
  ElMessage.success('报告已发布')
  loadList()
}

// ---------- 查看 ----------
const viewVisible = ref(false)
const viewData = ref(null)

async function openView(row) {
  const res = await getExamReport(row.reportId)
  viewData.value = res.data
  viewVisible.value = true
}

// ---------- 危急值 ----------
const criticalVisible = ref(false)
const criticalRemark = ref('')
let criticalReportId = null

function openCritical(row) {
  criticalReportId = row.reportId
  criticalRemark.value = ''
  criticalVisible.value = true
}

async function submitCritical() {
  if (!criticalRemark.value) {
    ElMessage.warning('请填写处理备注')
    return
  }
  await handleCritical(criticalReportId, criticalRemark.value)
  ElMessage.success('危急值已处理')
  criticalVisible.value = false
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.mt8 {
  margin-top: 8px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
