<template>
  <el-card shadow="never">
    <el-tabs v-model="tab" @tab-change="onTab">
      <!-- 健康档案 -->
      <el-tab-pane label="健康档案" name="record">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="姓名">
            <el-input v-model="recordQuery.keyword" placeholder="患者姓名" clearable style="width: 160px" @keyup.enter="loadRecords" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadRecords">查询</el-button>
            <el-button type="success" icon="Plus" @click="openRecord">新建档案</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="recordLoading" :data="records" border stripe>
          <el-table-column label="档案号" prop="recordNo" width="100" align="center" />
          <el-table-column label="姓名" prop="patientName" min-width="90" />
          <el-table-column label="性别" width="70" align="center">
            <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
          </el-table-column>
          <el-table-column label="慢病" prop="chronicDisease" min-width="120" />
          <el-table-column label="吸烟" width="70" align="center">
            <template #default="{ row }">{{ row.smoking === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="饮酒" width="70" align="center">
            <template #default="{ row }">{{ row.drinking === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="运动频率" prop="exercise" width="100" />
          <el-table-column label="建档时间" prop="createTime" min-width="150" />
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openVaccination(row)">接种记录</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-if="reminds.length" type="warning" :closable="false" class="mt8">
          <template #title>
            接种提醒（7日内）：
            <span v-for="r in reminds" :key="r.id" class="remind-tag">
              {{ r.patientName }} {{ r.vaccineName }} 第{{ r.doseNo }}剂 {{ r.nextDate }}
            </span>
          </template>
        </el-alert>
      </el-tab-pane>

      <!-- 传染病上报 -->
      <el-tab-pane label="传染病上报" name="infectious">
        <el-row class="mb8">
          <el-button type="danger" icon="Warning" @click="openInfectious">传染病上报</el-button>
        </el-row>
        <el-table v-loading="infLoading" :data="infectious" border stripe>
          <el-table-column label="患者" prop="patientName" min-width="90" />
          <el-table-column label="病名" prop="diseaseName" min-width="120" />
          <el-table-column label="ICD" prop="icdCode" width="90" align="center" />
          <el-table-column label="报告日期" prop="reportDate" width="110" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.reportStatus === 1 ? 'success' : 'info'">
                {{ row.reportStatus === 1 ? '已上报' : '待上报' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="疾控反馈" prop="cdcResponse" min-width="140" />
        </el-table>
      </el-tab-pane>

      <!-- 体检记录 -->
      <el-tab-pane label="体检记录" name="checkup">
        <el-row class="mb8">
          <el-button type="success" icon="Plus" @click="openCheckup">新增体检</el-button>
        </el-row>
        <el-table v-loading="chkLoading" :data="checkups" border stripe>
          <el-table-column label="体检单号" prop="checkupNo" width="110" align="center" />
          <el-table-column label="患者" prop="patientName" min-width="90" />
          <el-table-column label="类型" prop="checkupType" width="90" align="center" />
          <el-table-column label="日期" prop="checkupDate" width="110" align="center" />
          <el-table-column label="项目" prop="examItems" min-width="160" />
          <el-table-column label="结论" prop="conclusion" min-width="120" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="妇幼保健" name="maternal">
        <el-row class="mb8">
          <el-button type="primary" icon="Plus" @click="maternalVisible = true">孕产妇登记</el-button>
          <el-button type="success" icon="Plus" @click="childVisible = true">儿童保健登记</el-button>
        </el-row>
        <h4>孕产妇管理</h4>
        <el-table v-loading="maternalLoading" :data="maternalList" border stripe size="small">
          <el-table-column label="患者" prop="patientName" min-width="90" />
          <el-table-column label="末次月经" prop="lastPeriodDate" width="110" align="center" />
          <el-table-column label="预产期" prop="expectedDate" width="110" align="center" />
          <el-table-column label="产检次数" prop="checkupCount" width="90" align="center" />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">{{ row.status === 1 ? '孕中' : '已分娩' }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="120" />
        </el-table>
        <h4 class="mt8">儿童保健</h4>
        <el-table v-loading="childLoading" :data="childList" border stripe size="small">
          <el-table-column label="患者" prop="patientName" min-width="90" />
          <el-table-column label="出生体重kg" prop="birthWeight" width="100" align="center" />
          <el-table-column label="身高cm" prop="height" width="90" align="center" />
          <el-table-column label="体重kg" prop="weight" width="90" align="center" />
          <el-table-column label="检查日期" prop="checkDate" width="110" align="center" />
          <el-table-column label="备注" prop="remark" min-width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建/编辑档案 -->
    <el-dialog :title="recordForm.id ? '编辑档案' : '新建健康档案'" v-model="recordVisible" width="480px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="recordForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%" :disabled="!!recordForm.id">
            <el-option v-for="p in patientOptions" :key="p.id" :label="`${p.name}（${p.patientNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="慢病">
          <el-select v-model="recordForm.chronicList" multiple style="width: 100%">
            <el-option v-for="d in ['高血压', '糖尿病', '冠心病', '慢阻肺']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="吸烟">
          <el-radio-group v-model="recordForm.smoking"><el-radio :value="0">否</el-radio><el-radio :value="1">是</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="饮酒">
          <el-radio-group v-model="recordForm.drinking"><el-radio :value="0">否</el-radio><el-radio :value="1">是</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="运动频率">
          <el-select v-model="recordForm.exercise" style="width: 100%">
            <el-option v-for="e in ['每天', '每周1-2次', '偶尔', '从不']" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitRecord">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 接种记录 -->
    <el-dialog :title="`接种记录：${vaccPatient?.patientName || ''}`" v-model="vaccVisible" width="640px">
      <el-button type="primary" size="small" icon="Plus" class="mb8" @click="vaccAddVisible = true">新增接种</el-button>
      <el-table v-loading="vaccLoading" :data="vaccinations" border size="small">
        <el-table-column label="疫苗" prop="vaccineName" min-width="120" />
        <el-table-column label="批号" prop="batchNo" width="100" />
        <el-table-column label="剂次" prop="doseNo" width="60" align="center" />
        <el-table-column label="接种日期" prop="vaccinateDate" width="110" align="center" />
        <el-table-column label="下次接种" prop="nextDate" width="110" align="center" />
      </el-table>
    </el-dialog>

    <!-- 新增接种 -->
    <el-dialog title="新增接种" v-model="vaccAddVisible" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="疫苗名称" required><el-input v-model="vaccForm.vaccineName" /></el-form-item>
        <el-form-item label="批号"><el-input v-model="vaccForm.batchNo" /></el-form-item>
        <el-form-item label="剂次"><el-input-number v-model="vaccForm.doseNo" :min="1" /></el-form-item>
        <el-form-item label="接种日期"><el-date-picker v-model="vaccForm.vaccinateDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="下次接种"><el-date-picker v-model="vaccForm.nextDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vaccAddVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitVaccination">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 传染病上报 -->
    <el-dialog title="传染病上报（上报即建档）" v-model="infVisible" width="440px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="infForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="`${p.name}（${p.patientNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="病名" required><el-input v-model="infForm.diseaseName" placeholder="如：肺结核" /></el-form-item>
        <el-form-item label="ICD编码"><el-input v-model="infForm.icdCode" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infVisible = false">取 消</el-button>
        <el-button type="danger" @click="submitInfectious">确认上报</el-button>
      </template>
    </el-dialog>

    <!-- 新增体检 -->
    <el-dialog title="新增体检" v-model="chkVisible" width="480px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="chkForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="`${p.name}（${p.patientNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="chkForm.checkupType" style="width: 100%">
            <el-option v-for="t in ['入职', '常规', '老年']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目"><el-input v-model="chkForm.examItems" placeholder="逗号分隔，如：血常规,尿常规,胸片" /></el-form-item>
        <el-form-item label="结果摘要"><el-input v-model="chkForm.examResult" /></el-form-item>
        <el-form-item label="结论"><el-input v-model="chkForm.conclusion" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chkVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitCheckup">确 定</el-button>
      </template>
    </el-dialog>
    <!-- 孕产妇登记 -->
    <el-dialog title="孕产妇登记" v-model="maternalVisible" width="440px">
      <el-form label-width="90px">
        <el-form-item label="患者" required>
          <el-select v-model="maternalForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="末次月经"><el-date-picker v-model="maternalForm.lastPeriodDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="预产期"><el-date-picker v-model="maternalForm.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="maternalForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maternalVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitMaternal">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 儿童保健 -->
    <el-dialog title="儿童保健登记" v-model="childVisible" width="440px">
      <el-form label-width="90px">
        <el-form-item label="儿童" required>
          <el-select v-model="childForm.patientId" filterable remote :remote-method="searchPatients"
            :loading="searching" placeholder="输入姓名搜索" style="width: 100%">
            <el-option v-for="p in patientOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生体重kg"><el-input-number v-model="childForm.birthWeight" :precision="2" :step="0.1" /></el-form-item>
        <el-form-item label="身高cm"><el-input-number v-model="childForm.height" :precision="1" /></el-form-item>
        <el-form-item label="体重kg"><el-input-number v-model="childForm.weight" :precision="2" :step="0.1" /></el-form-item>
        <el-form-item label="检查日期"><el-date-picker v-model="childForm.checkDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="childVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitChild">确 定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getHealthRecords, createHealthRecord, updateHealthRecord, getVaccinations,
  addVaccination, reportInfectious, getInfectiousPage, getCheckupPage, addCheckup,
  getPatientPage, getVaccinationRemind,
  getMaternalPage, addMaternal, getChildCarePage, addChildCare
} from '@/api/system'

const tab = ref('record')
const records = ref([])
const recordLoading = ref(false)
const recordQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const reminds = ref([])

async function loadRecords() {
  recordLoading.value = true
  try {
    const res = await getHealthRecords(recordQuery)
    records.value = res.data.list
  } finally {
    recordLoading.value = false
  }
}

// ---------- 档案 ----------
const recordVisible = ref(false)
const recordForm = reactive({
  id: null, patientId: null, chronicList: [], smoking: 0, drinking: 0, exercise: '偶尔'
})
const patientOptions = ref([])
const searching = ref(false)

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

function openRecord() {
  Object.assign(recordForm, { id: null, patientId: null, chronicList: [], smoking: 0, drinking: 0, exercise: '偶尔' })
  recordVisible.value = true
}

function openEdit(row) {
  Object.assign(recordForm, {
    id: row.id, patientId: row.patientId,
    chronicList: (row.chronicDisease || '').split(',').filter(Boolean),
    smoking: row.smoking, drinking: row.drinking, exercise: row.exercise
  })
  recordVisible.value = true
}

async function submitRecord() {
  if (!recordForm.patientId) {
    ElMessage.warning('请选择患者')
    return
  }
  const payload = {
    patientId: recordForm.patientId,
    chronicDisease: recordForm.chronicList.join(','),
    smoking: recordForm.smoking, drinking: recordForm.drinking, exercise: recordForm.exercise
  }
  if (recordForm.id) {
    await updateHealthRecord(recordForm.id, payload)
    ElMessage.success('档案已更新')
  } else {
    await createHealthRecord(payload)
    ElMessage.success('建档成功')
  }
  recordVisible.value = false
  loadRecords()
}

// ---------- 接种 ----------
const vaccVisible = ref(false)
const vaccAddVisible = ref(false)
const vaccLoading = ref(false)
const vaccinations = ref([])
const vaccPatient = ref(null)
const vaccForm = reactive({ vaccineName: '', batchNo: '', doseNo: 1, vaccinateDate: null, nextDate: null })

async function openVaccination(row) {
  vaccPatient.value = row
  vaccVisible.value = true
  vaccLoading.value = true
  try {
    const res = await getVaccinations({ pageNum: 1, pageSize: 50, patientId: row.patientId })
    vaccinations.value = res.data.list
  } finally {
    vaccLoading.value = false
  }
}

async function submitVaccination() {
  if (!vaccForm.vaccineName) {
    ElMessage.warning('请输入疫苗名称')
    return
  }
  await addVaccination({ ...vaccForm, patientId: vaccPatient.value.patientId })
  ElMessage.success('接种已登记')
  vaccAddVisible.value = false
  openVaccination(vaccPatient.value)
  loadReminds()
}

async function loadReminds() {
  const res = await getVaccinationRemind()
  reminds.value = res.data
}

// ---------- 传染病 ----------
const infVisible = ref(false)
const infLoading = ref(false)
const infectious = ref([])
const infForm = reactive({ patientId: null, diseaseName: '', icdCode: '' })

async function loadInfectious() {
  infLoading.value = true
  try {
    const res = await getInfectiousPage({ pageNum: 1, pageSize: 50 })
    infectious.value = res.data.list
  } finally {
    infLoading.value = false
  }
}

function openInfectious() {
  Object.assign(infForm, { patientId: null, diseaseName: '', icdCode: '' })
  infVisible.value = true
}

async function submitInfectious() {
  if (!infForm.patientId || !infForm.diseaseName) {
    ElMessage.warning('请选择患者并填写病名')
    return
  }
  await reportInfectious(infForm)
  ElMessage.success('上报成功')
  infVisible.value = false
  loadInfectious()
}

// ---------- 体检 ----------
const chkVisible = ref(false)
const chkLoading = ref(false)
const checkups = ref([])
const chkForm = reactive({ patientId: null, checkupType: '常规', examItems: '', examResult: '', conclusion: '' })

async function loadCheckups() {
  chkLoading.value = true
  try {
    const res = await getCheckupPage({ pageNum: 1, pageSize: 50 })
    checkups.value = res.data.list
  } finally {
    chkLoading.value = false
  }
}

function openCheckup() {
  Object.assign(chkForm, { patientId: null, checkupType: '常规', examItems: '', examResult: '', conclusion: '' })
  chkVisible.value = true
}

async function submitCheckup() {
  if (!chkForm.patientId) {
    ElMessage.warning('请选择患者')
    return
  }
  await addCheckup(chkForm)
  ElMessage.success('体检已登记')
  chkVisible.value = false
  loadCheckups()
}

function onTab(t) {
  if (t === 'infectious') loadInfectious()
  if (t === 'checkup') loadCheckups()
  if (t === 'maternal') loadMaternal()
}

// ---------- 妇幼保健 ----------
const maternalVisible = ref(false)
const childVisible = ref(false)
const maternalLoading = ref(false)
const childLoading = ref(false)
const maternalList = ref([])
const childList = ref([])
const maternalForm = reactive({ patientId: null, lastPeriodDate: '', expectedDate: '', checkupCount: 0, remark: '' })
const childForm = reactive({ patientId: null, birthWeight: 3.4, height: 90, weight: 13, checkDate: '', remark: '' })

async function loadMaternal() {
  maternalLoading.value = true
  childLoading.value = true
  try {
    const m = await getMaternalPage({ pageNum: 1, pageSize: 50 })
    maternalList.value = m.data.list
    const c = await getChildCarePage({ pageNum: 1, pageSize: 50 })
    childList.value = c.data.list
  } finally {
    maternalLoading.value = false
    childLoading.value = false
  }
}

async function submitMaternal() {
  if (!maternalForm.patientId) {
    ElMessage.warning('请选择患者')
    return
  }
  await addMaternal(maternalForm)
  ElMessage.success('孕产妇已登记')
  maternalVisible.value = false
  loadMaternal()
}

async function submitChild() {
  if (!childForm.patientId) {
    ElMessage.warning('请选择患者')
    return
  }
  await addChildCare(childForm)
  ElMessage.success('儿童保健已登记')
  childVisible.value = false
  loadMaternal()
}

onMounted(async () => {
  loadRecords()
  loadReminds()
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.mt8 {
  margin-top: 8px;
}

.remind-tag {
  margin-right: 12px;
}
</style>
