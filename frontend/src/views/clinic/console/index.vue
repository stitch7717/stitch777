<template>
  <div class="console">
    <!-- 左：就诊列表 -->
    <el-card shadow="never" class="visit-list">
      <template #header>
        <div class="card-head">
          <span>就诊列表（今日）</span>
          <el-button link type="primary" icon="Refresh" @click="loadVisits" />
        </div>
      </template>
      <el-table
        v-loading="visitsLoading"
        :data="visits"
        highlight-current-row
        height="560"
        @current-change="onSelectVisit"
      >
        <el-table-column label="排队号" prop="queueNo" width="70" align="center" />
        <el-table-column label="姓名" prop="patientName" min-width="80" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.status !== 0"
              @click.stop="doReceive(row)"
            >接诊</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 右：接诊详情 -->
    <el-card shadow="never" class="visit-detail">
      <template #header>
        <span>接诊详情</span>
      </template>
      <template v-if="currentVisit">
        <!-- 患者信息条 -->
        <div class="patient-bar">
          <b>{{ currentVisit.patientName }}</b>
          <span>{{ currentVisit.gender === 1 ? '男' : '女' }}</span>
          <span v-if="currentVisit.age != null">{{ currentVisit.age }} 岁</span>
          <span>{{ currentVisit.regNo }}</span>
          <span class="allergy" v-if="currentVisit.allergyHistory">
            ⚠ 过敏：{{ currentVisit.allergyHistory }}
          </span>
        </div>

        <!-- 病历表单 -->
        <el-form :model="recordForm" label-width="70px" class="record-form">
          <el-form-item label="主诉">
            <el-input v-model="recordForm.chiefComplaint" placeholder="如：发热3天" />
          </el-form-item>
          <el-form-item label="现病史">
            <el-input v-model="recordForm.presentIllness" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="体格检查">
            <el-input v-model="recordForm.physicalExam" type="textarea" :rows="2" placeholder="如：体温38.5℃，咽部充血" />
          </el-form-item>
          <el-form-item label="诊断">
            <el-input v-model="recordForm.diagnosis" placeholder="如：上呼吸道感染" />
          </el-form-item>
          <el-form-item label="ICD编码">
            <el-input v-model="recordForm.icdCode" placeholder="如：J06" style="width: 200px" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :disabled="recordLocked"
              @click="doSaveRecord(true)"
            >保存并提交病历</el-button>
            <el-button :disabled="recordLocked" @click="doSaveRecord(false)">存草稿</el-button>
            <el-tag v-if="recordLocked" type="info" class="locked-tag">病历已提交，不可修改</el-tag>
          </el-form-item>
        </el-form>

        <!-- 处方 -->
        <div class="presc-head">
          <h4>处方</h4>
          <el-button
            type="primary"
            size="small"
            icon="Plus"
            :disabled="!canPrescribe"
            @click="openPrescDialog"
          >开处方</el-button>
        </div>
        <el-table :data="prescriptions" border size="small">
          <el-table-column label="处方号" prop="prescNo" min-width="130" />
          <el-table-column label="明细" min-width="220">
            <template #default="{ row }">
              <div v-for="it in row.items" :key="it.id" class="item-line">
                {{ it.drugName }} {{ it.spec }} ×{{ it.quantity }}（{{ it.usage }} {{ it.frequency }}）
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="90" align="center">
            <template #default="{ row }">￥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="prescStatusType(row.status)">
                {{ prescStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                v-if="row.status === 0 && canAudit"
                @click="doAudit(row)"
              >审核</el-button>
              <el-button
                link
                type="danger"
                size="small"
                v-if="row.status === 0"
                @click="doVoid(row)"
              >作废</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="请在左侧选择就诊患者" :image-size="80" />
    </el-card>

    <!-- 开处方弹窗 -->
    <el-dialog title="开处方" v-model="prescDialogVisible" width="860px" :close-on-click-modal="false">
      <el-form label-width="70px">
        <el-form-item label="处方类型">
          <el-radio-group v-model="prescForm.prescType">
            <el-radio :value="1">西药</el-radio>
            <el-radio :value="2">中药</el-radio>
            <el-radio :value="3">中成药</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="诊断">
          <el-input v-model="prescForm.diagnosis" :placeholder="recordForm.diagnosis || '请输入诊断'" />
        </el-form-item>
      </el-form>

      <el-table :data="prescForm.items" border size="small">
        <el-table-column label="药品" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.drugId" placeholder="选择药品" filterable style="width: 100%">
              <el-option
                v-for="d in drugOptions"
                :key="d.id"
                :label="`${d.name}（${d.spec}）￥${d.salePrice}`"
                :value="d.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="用法" width="110">
          <template #default="{ row }">
            <el-select v-model="row.usage" placeholder="口服" clearable>
              <el-option v-for="u in ['口服', '静滴', '肌注', '外用']" :key="u" :label="u" :value="u" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="用量" width="100">
          <template #default="{ row }">
            <el-input v-model="row.dosage" placeholder="如 1粒" />
          </template>
        </el-table-column>
        <el-table-column label="频次" width="110">
          <template #default="{ row }">
            <el-select v-model="row.frequency" placeholder="每日3次" clearable>
              <el-option v-for="f in ['每日1次', '每日2次', '每日3次', '必要时']" :key="f" :label="f" :value="f" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="天数" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.days" :min="1" :max="90" :controls="false" style="width: 70px" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="90">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="100" :controls="false" style="width: 70px" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="80" align="center">
          <template #default="{ row }">{{ itemAmount(row) }}</template>
        </el-table-column>
        <el-table-column label="" width="60" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" icon="Delete" @click="prescForm.items.splice($index, 1)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="presc-dialog-foot">
        <el-button link type="primary" icon="Plus" @click="addPrescItem">添加药品</el-button>
        <span class="total">合计：<b>￥{{ totalAmount }}</b></span>
      </div>

      <template #footer>
        <el-button @click="prescDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPrescription">提交处方</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVisitList,
  receiveVisit,
  saveRecord,
  getRecordByVisitNo,
  createPrescription,
  getPrescriptionPage,
  auditPrescription,
  voidPrescription,
  getClinicDrugs
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const roleCode = computed(() => userStore.userInfo?.roleCode || '')
const canAudit = computed(() => ['admin', 'dean', 'doctor'].includes(roleCode.value))

const visitsLoading = ref(false)
const visits = ref([])
const currentVisit = ref(null)

const recordForm = reactive({
  chiefComplaint: '',
  presentIllness: '',
  physicalExam: '',
  diagnosis: '',
  icdCode: ''
})
const recordLocked = ref(false)
const recordStatus = ref(-1)

const prescriptions = ref([])
const drugOptions = ref([])

const prescDialogVisible = ref(false)
const saving = ref(false)
const prescForm = reactive({
  prescType: 1,
  diagnosis: '',
  items: []
})

const canPrescribe = computed(
  () => currentVisit.value && (currentVisit.value.status === 1 || currentVisit.value.status === 2)
)

function statusText(s) {
  return { 0: '候诊', 1: '就诊中', 2: '已完成', 3: '已退号' }[s] || '-'
}
function statusType(s) {
  return { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info'
}
function prescStatusText(s) {
  return { 0: '待审核', 1: '已审核', 2: '已收费', 3: '已发药', 4: '已退' }[s] || '-'
}
function prescStatusType(s) {
  return { 0: 'info', 1: 'success', 2: 'warning', 3: 'success', 4: 'danger' }[s] || 'info'
}

async function loadVisits() {
  visitsLoading.value = true
  try {
    const res = await getVisitList({})
    visits.value = res.data
  } finally {
    visitsLoading.value = false
  }
}

async function onSelectVisit(row) {
  // 表格刷新会触发 current-change(null)，忽略避免丢失选中详情
  if (!row) return
  currentVisit.value = row
  recordLocked.value = false
  recordStatus.value = -1
  Object.assign(recordForm, {
    chiefComplaint: '',
    presentIllness: '',
    physicalExam: '',
    diagnosis: '',
    icdCode: ''
  })
  const recRes = await getRecordByVisitNo(row.regNo)
  if (recRes.data && recRes.data.id) {
    recordStatus.value = recRes.data.status
    recordLocked.value = recRes.data.status === 1
    Object.assign(recordForm, {
      chiefComplaint: recRes.data.chiefComplaint || '',
      presentIllness: recRes.data.presentIllness || '',
      physicalExam: recRes.data.physicalExam || '',
      diagnosis: recRes.data.diagnosis || '',
      icdCode: recRes.data.icdCode || ''
    })
  }
  loadPrescriptions()
}

async function loadPrescriptions() {
  if (!currentVisit.value) return
  const res = await getPrescriptionPage({ pageNum: 1, pageSize: 20, visitId: currentVisit.value.id })
  prescriptions.value = res.data.list
}

async function doReceive(row) {
  await receiveVisit(row.id)
  ElMessage.success(`已接诊：${row.patientName}`)
  await loadVisits()
  const fresh = visits.value.find((v) => v.id === row.id)
  if (fresh) {
    currentVisit.value = null
    // 重新选中
    setTimeout(() => onSelectVisit(fresh), 50)
  }
}

async function doSaveRecord(submit) {
  if (submit && !recordForm.diagnosis) {
    ElMessage.warning('提交病历前请填写诊断')
    return
  }
  await saveRecord({ ...recordForm, visitNo: currentVisit.value.regNo, submit })
  ElMessage.success(submit ? '病历已提交，就诊完成' : '草稿已保存')
  if (submit) recordLocked.value = true
  loadVisits()
}

function openPrescDialog() {
  prescForm.prescType = 1
  prescForm.diagnosis = recordForm.diagnosis || ''
  prescForm.items = [emptyItem()]
  if (!drugOptions.value.length) {
    getClinicDrugs().then((res) => (drugOptions.value = res.data))
  }
  prescDialogVisible.value = true
}

function emptyItem() {
  return { drugId: null, usage: '口服', dosage: '', frequency: '每日3次', days: 3, quantity: 1 }
}

function addPrescItem() {
  prescForm.items.push(emptyItem())
}

function itemAmount(row) {
  const drug = drugOptions.value.find((d) => d.id === row.drugId)
  return drug ? (drug.salePrice * (row.quantity || 0)).toFixed(2) : '0.00'
}

const totalAmount = computed(() =>
  prescForm.items.reduce((sum, it) => sum + Number(itemAmount(it)), 0).toFixed(2)
)

async function submitPrescription() {
  const valid = prescForm.items.filter((it) => it.drugId && it.quantity > 0)
  if (!valid.length) {
    ElMessage.warning('请至少选择一种药品并填写数量')
    return
  }
  saving.value = true
  try {
    const res = await createPrescription({
      visitId: currentVisit.value.id,
      prescType: prescForm.prescType,
      diagnosis: prescForm.diagnosis,
      items: valid
    })
    ElMessage.success(`处方已开立：${res.data.prescNo}，合计 ￥${res.data.totalAmount}`)
    prescDialogVisible.value = false
    loadPrescriptions()
  } finally {
    saving.value = false
  }
}

async function doAudit(row) {
  await auditPrescription(row.id)
  ElMessage.success('审核通过')
  loadPrescriptions()
}

async function doVoid(row) {
  await ElMessageBox.confirm(`确认作废处方 ${row.prescNo}？`, '提示', { type: 'warning' })
  await voidPrescription(row.id)
  ElMessage.success('已作废')
  loadPrescriptions()
}

onMounted(loadVisits)
</script>

<style scoped>
.console {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.visit-list {
  width: 420px;
  flex-shrink: 0;
}

.visit-detail {
  flex: 1;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.patient-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 14px;
  font-size: 14px;
}

.patient-bar .allergy {
  color: #f56c6c;
  font-weight: 600;
  margin-left: auto;
}

.record-form {
  margin-bottom: 10px;
}

.locked-tag {
  margin-left: 12px;
}

.presc-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0 10px;
}

.item-line {
  line-height: 1.7;
  font-size: 12px;
}

.presc-dialog-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.presc-dialog-foot .total b {
  color: #f56c6c;
  font-size: 16px;
}
</style>
