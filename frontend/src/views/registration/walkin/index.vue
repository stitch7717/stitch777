<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab">
      <!-- ================= 现场挂号 ================= -->
      <el-tab-pane label="现场挂号" name="register">
        <el-form :inline="true" label-width="70px" @submit.prevent>
          <el-form-item label="姓名" required>
            <el-autocomplete
              v-model="regPatientForm.name"
              :fetch-suggestions="fetchNameSuggestions"
              @input="onNameInput"
              placeholder="输入姓名搜索，或直接新建"
              style="width: 220px"
              value-key="name"
              :trigger-on-focus="false"
              :debounce="300"
              @select="handleNameSelect"
              clearable
            >
              <template #default="{ item }">
                <div style="display: flex; justify-content: space-between; width: 100%">
                  <span>{{ item.name }}（{{ item.patientNo }}）</span>
                  <span style="color: #a8abb2; font-size: 12px">{{ item.phone }}</span>
                </div>
              </template>
            </el-autocomplete>
          </el-form-item>
          <el-form-item label="手机号" required>
            <el-input
              v-model="regPatientForm.phone"
              placeholder="新患者请填手机号"
              style="width: 170px"
              :disabled="patientFound"
              @blur="lookupPatient"
              @keyup.enter="lookupPatient"
            />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="regPatientForm.gender" :disabled="patientFound">
              <el-radio :value="1">男</el-radio><el-radio :value="0">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="patientFound">
            <el-tag type="success" size="small" effect="plain">已有档案：{{ foundLabel }}，直接使用</el-tag>
          </el-form-item>
          <el-form-item label="科室">
            <el-select
              v-model="regForm.deptId"
              placeholder="请选择科室"
              style="width: 150px"
              @change="onDeptChange"
            >
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="医生">
            <el-select
              v-model="regForm.doctorId"
              placeholder="请选择医生"
              style="width: 150px"
              :disabled="!regForm.deptId"
              @change="loadSchedules"
            >
              <el-option v-for="d in doctors" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker
              v-model="regForm.date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 150px"
              @change="loadSchedules"
            />
          </el-form-item>
          <el-form-item label="时段">
            <el-radio-group v-model="regForm.period" size="small" @change="loadSchedules">
              <el-radio-button :value="null">全部</el-radio-button>
              <el-radio-button v-for="p in periodOptions" :key="p.value" :value="p.value">
                {{ p.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <!-- 可选排班卡片 -->
        <div class="schedule-cards">
          <div
            v-for="s in schedules"
            :key="s.id"
            class="schedule-card"
            :class="{ selected: regForm.scheduleId === s.id, stopped: s.status === 0, full: s.remainNum <= 0 }"
            @click="selectSchedule(s)"
          >
            <div class="card-line">
              <el-tag :type="['', 'info', 'warning', 'danger'][s.regType]" size="small">
                {{ ['', '普通', '专家', '急诊'][s.regType] }}
              </el-tag>
              <span class="fee">￥{{ s.regFee }}</span>
            </div>
            <div class="card-line muted">{{ s.scheduleDate }} · {{ periodText(s.timePeriod) }}</div>
            <div class="card-line muted">
              余号 <b :class="{ danger: s.remainNum <= 0 }">{{ s.remainNum }}</b> / {{ s.totalNum }}
              <el-tag v-if="s.status === 0" type="danger" size="small">停诊</el-tag>
            </div>
          </div>
          <el-empty v-if="!schedules.length" description="暂无排班，请先选择科室/医生/日期" :image-size="60" />
        </div>

        <div class="reg-footer">
          <span v-if="selectedSchedule" class="fee-summary">
            挂号费：<b>￥{{ selectedSchedule.regFee }}</b>
          </span>
          <el-button
            type="primary"
            :disabled="!regForm.scheduleId || (!regForm.patientId && (!regPatientForm.phone || !regPatientForm.name))"
            :loading="registering"
            @click="doRegister"
          >
            确认挂号
          </el-button>
        </div>
      </el-tab-pane>

      <!-- ================= 排班管理 ================= -->
      <el-tab-pane label="排班管理" name="schedule">
        <el-form :inline="true" :model="schForm" @submit.prevent>
          <el-form-item label="科室">
            <el-select v-model="schForm.deptId" placeholder="科室" style="width: 140px" @change="onDeptChange">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="医生">
            <el-select v-model="schForm.doctorId" placeholder="医生" style="width: 140px">
              <el-option v-for="d in allDoctors" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker
              v-model="schForm.scheduleDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="时段">
            <el-select v-model="schForm.timePeriod" style="width: 150px">
              <el-option v-for="p in periodOptions" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="号别">
            <el-select v-model="schForm.regType" style="width: 110px">
              <el-option label="普通" :value="1" />
              <el-option label="专家" :value="2" />
              <el-option label="急诊" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="号源">
            <el-input-number v-model="schForm.totalNum" :min="1" :max="200" />
          </el-form-item>
          <el-form-item label="挂号费">
            <el-input-number v-model="schForm.regFee" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Plus" @click="doCreateSchedule">新增排班</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="schLoading" :data="scheduleList" border stripe>
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="日期" prop="scheduleDate" width="110" align="center" />
          <el-table-column label="时段" width="80" align="center">
            <template #default="{ row }">{{ periodText(row.timePeriod) }}</template>
          </el-table-column>
          <el-table-column label="科室" prop="deptName" min-width="90" />
          <el-table-column label="医生" prop="doctorName" min-width="90" />
          <el-table-column label="号别" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="['', 'info', 'warning', 'danger'][row.regType]" size="small">
                {{ ['', '普通', '专家', '急诊'][row.regType] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="余号/总号" width="100" align="center">
            <template #default="{ row }">{{ row.remainNum }}/{{ row.totalNum }}</template>
          </el-table-column>
          <el-table-column label="挂号费" width="90" align="center">
            <template #default="{ row }">￥{{ row.regFee }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '正常' : '停诊' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="danger"
                :disabled="row.status === 0"
                @click="doStop(row)"
              >停诊</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ================= 候诊队列 ================= -->
      <el-tab-pane label="候诊队列" name="queue">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="科室">
            <el-select v-model="queueDeptId" placeholder="请选择科室" style="width: 180px">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadQueue">查询</el-button>
            <el-button
              type="warning"
              icon="Bell"
              :disabled="!firstWaiting"
              @click="callNext"
            >
              叫下一位{{ firstWaiting ? `（${firstWaiting.queueNo} 号 ${firstWaiting.patientName}）` : '' }}
            </el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="queueLoading" :data="queueList" border stripe>
          <el-table-column label="排队号" prop="queueNo" width="90" align="center">
            <template #default="{ row }">
              <span :class="{ 'now-serving': row.status === 1 }">{{ row.queueNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" prop="patientName" min-width="100" />
          <el-table-column label="挂号单号" prop="regNo" min-width="150" />
          <el-table-column label="号别" width="80" align="center">
            <template #default="{ row }">{{ ['', '普通', '专家', '急诊'][row.regType] }}</template>
          </el-table-column>
          <el-table-column label="医生" prop="doctorName" min-width="90" />
          <el-table-column label="挂号时间" prop="regTime" min-width="160" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'warning' : 'info'" size="small">
                {{ ['', '就诊中', '已完成', '已退号'][row.status] || '候诊中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="row.status !== 0"
                @click="doCall(row)"
              >叫号</el-button>
              <el-button
                link
                type="danger"
                :disabled="row.status !== 0"
                @click="doCancel(row)"
              >退号</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <!-- 查找患者 -->
    <el-dialog title="查找患者" v-model="searchVisible" width="560px" append-to-body>
      <el-input v-model="searchKeyword" placeholder="姓名/手机号" clearable @keyup.enter="doSearchPatients">
        <template #append><el-button icon="Search" @click="doSearchPatients" /></template>
      </el-input>
      <el-table v-loading="searchLoading" :data="searchResults" border size="small" class="mt8" max-height="320"
        highlight-current-row @current-change="(r) => (r ? pickPatient(r) : null)">
        <el-table-column label="编号" prop="patientNo" width="90" align="center" />
        <el-table-column label="姓名" prop="name" min-width="90" />
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" min-width="120" />
        <el-table-column label="就诊卡" prop="cardNo" min-width="100" />
      </el-table>
      <p class="tip" style="margin-top: 8px">点击行即可选用该患者档案</p>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getScheduleList,
  createSchedule,
  stopSchedule,
  register,
  cancelRegistration,
  getQueue,
  callNumber,
  getRegDepts,
  getRegDoctors,
  getPatientPage,
  createPatient,
  registerOrGet
} from '@/api/system'

const activeTab = ref('register')

// ---------- 公共下拉 ----------
const depts = ref([])
const doctors = ref([])
const allDoctors = ref([])

// ---------- 现场挂号 ----------
const regForm = reactive({
  patientId: null,
  deptId: null,
  doctorId: null,
  date: null,
  period: null,
  scheduleId: null
})

// 患者：手机号自动识别（有档案复用，无档案登记）
const regPatientForm = reactive({ name: '', gender: 1, phone: '' })
const patientFound = ref(false)
const foundLabel = ref('')
const searchVisible = ref(false)

/** 姓名边输边搜：部分字符即可实时出候选（前3后4比对手机号精确识别） */
async function fetchNameSuggestions(query, cb) {
  const kw = (query || '').trim()
  if (!kw) {
    cb([])
    return
  }
  const res = await getPatientPage({ pageNum: 1, pageSize: 10, keyword: kw })
  cb((res.data.list || []).map((x) => ({ ...x, value: x.name })))
}

/** 手动编辑姓名时：退出已选档案状态，解锁手机号 */
function onNameInput(val) {
  if (selecting.value) return
  if (patientFound.value && val !== foundLabel.value && val !== selectedName.value) {
    patientFound.value = false
    regForm.patientId = null
    regPatientForm.phone = ''
    foundLabel.value = ''
    selectedName.value = ''
  }
}

/** 选中候选：直接复用已有档案 */
function handleNameSelect(item) {
  if (item && item.id) {
    selecting.value = true
    regForm.patientId = item.id
    selectedName.value = item.name
    regPatientForm.phone = item.phone || ''
    regPatientForm.gender = item.gender
    patientFound.value = true
    foundLabel.value = `${item.name}（${item.patientNo}）`
    ElMessage.info(`已选用已有档案：${item.name}（${item.patientNo}）`)
    setTimeout(() => (selecting.value = false), 100)
  }
}

const selecting = ref(false)
const selectedName = ref('')

/** 手机号失焦：防止新患者手机号与已有档案撞号（前3后4比对） */
async function lookupPatient() {
  const phone = (regPatientForm.phone || '').trim()
  patientFound.value = false
  foundLabel.value = ''
  regForm.patientId = null
  if (!phone || !regPatientForm.name) return
  const res = await getPatientPage({ pageNum: 1, pageSize: 10, keyword: phone })
  const hit = (res.data.list || []).find((x) => {
    const p = (x.phone || '').replace(/\*/g, '')
    return p.length >= 7 && phone.startsWith(p.slice(0, 3)) && phone.endsWith(p.slice(-4))
  })
  if (hit) {
    regPatientForm.name = hit.name
    regPatientForm.gender = hit.gender
    regForm.patientId = hit.id
    patientFound.value = true
    foundLabel.value = `${hit.name}（${hit.patientNo}）`
    ElMessage.info(`该手机号已有档案：${hit.name}（${hit.patientNo}），直接使用`)
  }
}

/** 打开查找弹窗并按关键字搜索 */
async function openSearch() {
  searchVisible.value = true
  await doSearchPatients()
}

const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)

async function doSearchPatients() {
  searchLoading.value = true
  try {
    const res = await getPatientPage({
      pageNum: 1, pageSize: 20,
      keyword: searchKeyword.value || null
    })
    searchResults.value = res.data.list
  } finally {
    searchLoading.value = false
  }
}

function pickPatient(row) {
  regPatientForm.phone = row.phone || ''
  regPatientForm.name = row.name
  regPatientForm.gender = row.gender
  regForm.patientId = row.id
  patientFound.value = true
  foundLabel.value = `${row.name}（${row.patientNo}）`
  searchVisible.value = false
  ElMessage.info(`已选择：${row.name}（${row.patientNo}）`)
}

// 四时段：上午/下午各两节
const periodOptions = [
  { value: 1, label: '上午 8:00-10:00' },
  { value: 2, label: '上午 10:00-12:00' },
  { value: 3, label: '下午 14:00-16:00' },
  { value: 4, label: '下午 16:00-18:00' }
]
const patientOptions = ref([])
const patientSearching = ref(false)
const schedules = ref([])
const registering = ref(false)

const selectedSchedule = computed(() =>
  schedules.value.find((s) => s.id === regForm.scheduleId)
)

async function searchPatients(keyword) {
  if (!keyword) return
  patientSearching.value = true
  try {
    const res = await getPatientPage({ pageNum: 1, pageSize: 20, keyword })
    patientOptions.value = res.data.list
  } finally {
    patientSearching.value = false
  }
}

function onDeptChange() {
  regForm.doctorId = null
  loadDoctors().then(loadSchedules)
}

async function loadDoctors() {
  const res = await getRegDoctors(regForm.deptId)
  doctors.value = res.data
}

async function loadSchedules() {
  if (!regForm.doctorId) {
    schedules.value = []
    return
  }
  const params = { deptId: regForm.deptId, doctorId: regForm.doctorId }
  if (regForm.date) {
    params.startDate = regForm.date
    params.endDate = regForm.date
  }
  const res = await getScheduleList(params)
  // 时段前端过滤（1上午一 2上午二 3下午一 4下午二）
  schedules.value = regForm.period
    ? res.data.filter((x) => x.timePeriod === regForm.period)
    : res.data
}

function selectSchedule(s) {
  if (s.status === 0 || s.remainNum <= 0) return
  regForm.scheduleId = s.id
}

async function doRegister() {
  registering.value = true
  try {
    let patientId = regForm.patientId
    if (!patientId) {
      if (!regPatientForm.phone || !regPatientForm.name) {
        ElMessage.warning('请填写患者手机号与姓名')
        return
      }
      // 按手机号查重：已有档案直接复用，没有才新建
      const reg = await registerOrGet(regPatientForm)
      patientId = reg.data.id
      ElMessage.success(reg.data.created
        ? `已登记新患者：${reg.data.name}（${reg.data.patientNo}）`
        : `已有档案，直接使用：${reg.data.name}（${reg.data.patientNo}）`)
    }
    const res = await register({
      patientId,
      scheduleId: regForm.scheduleId
    })
    ElMessage.success(
      `挂号成功：${res.data.patientName} ${res.data.regNo}，排队号 ${res.data.queueNo}，应收 ￥${res.data.regFee}`
    )
    regForm.scheduleId = null
    loadSchedules()
  } finally {
    registering.value = false
  }
}

function periodText(p) {
  return {
    1: '上午 8:00-10:00',
    2: '上午 10:00-12:00',
    3: '下午 14:00-16:00',
    4: '下午 16:00-18:00'
  }[p] || '-'
}

// ---------- 排班管理 ----------
const schForm = reactive({
  deptId: null,
  doctorId: null,
  scheduleDate: null,
  timePeriod: 1,
  regType: 1,
  totalNum: 20,
  regFee: 20
})
const scheduleList = ref([])
const schLoading = ref(false)

async function loadScheduleList() {
  schLoading.value = true
  try {
    const res = await getScheduleList({})
    scheduleList.value = res.data
  } finally {
    schLoading.value = false
  }
}

async function doCreateSchedule() {
  if (!schForm.deptId || !schForm.doctorId || !schForm.scheduleDate) {
    ElMessage.warning('请先选择科室、医生与日期')
    return
  }
  await createSchedule({ ...schForm })
  ElMessage.success('排班创建成功')
  loadScheduleList()
}

async function doStop(row) {
  await ElMessageBox.confirm(
    `确认停诊：${row.scheduleDate} ${periodText(row.timePeriod)} ${row.doctorName}？`,
    '提示',
    { type: 'warning' }
  )
  await stopSchedule(row.id)
  ElMessage.success('已停诊')
  loadScheduleList()
}

// ---------- 候诊队列 ----------
const queueDeptId = ref(null)
const queueList = ref([])
const queueLoading = ref(false)

const firstWaiting = computed(() => queueList.value.find((q) => q.status === 0))

async function loadQueue() {
  if (!queueDeptId.value) {
    ElMessage.warning('请选择科室')
    return
  }
  queueLoading.value = true
  try {
    const res = await getQueue(queueDeptId.value)
    queueList.value = res.data
  } finally {
    queueLoading.value = false
  }
}

async function doCall(row) {
  await callNumber(row.id)
  ElMessage.success(`已叫号：${row.queueNo} 号 ${row.patientName}`)
  loadQueue()
}

async function callNext() {
  if (!firstWaiting.value) return
  await doCall(firstWaiting.value)
}

async function doCancel(row) {
  await ElMessageBox.confirm(
    `确认退号：${row.patientName}（${row.regNo}，${row.queueNo} 号）？`,
    '提示',
    { type: 'warning' }
  )
  await cancelRegistration(row.id)
  ElMessage.success('退号成功')
  loadQueue()
}

onMounted(async () => {
  const [deptRes, docRes] = await Promise.all([getRegDepts(), getRegDoctors()])
  depts.value = deptRes.data
  allDoctors.value = docRes.data
  doctors.value = docRes.data
  loadScheduleList()
})
</script>

<style scoped>
.schedule-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 120px;
  margin: 8px 0 16px;
}

.schedule-card {
  width: 190px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.schedule-card:hover {
  border-color: #1f6feb;
}

.schedule-card.selected {
  border-color: #1f6feb;
  background: #ecf5ff;
}

.schedule-card.stopped,
.schedule-card.full {
  opacity: 0.55;
  cursor: not-allowed;
}

.card-line {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-line .fee {
  margin-left: auto;
  color: #f56c6c;
  font-weight: 600;
}

.card-line.muted {
  color: #909399;
  font-size: 12px;
}

.card-line b.danger {
  color: #f56c6c;
}

.reg-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.fee-summary b {
  color: #f56c6c;
  font-size: 16px;
}

.now-serving {
  color: #e6a23c;
  font-weight: 700;
  font-size: 16px;
}
</style>
